package com.sns.identity.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableConfigurationProperties(SnsJwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SnsKeySource snsKeySource(SnsJwtProperties props) {
        return SnsKeySource.fromProperties(props);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Decoder validates the signature with our RS256 public key plus:
     * <ul>
     *   <li>Default Spring validators — exp / nbf with the configured clock skew.</li>
     *   <li>Issuer validator pinned to {@code sns.jwt.issuer}.</li>
     *   <li>Audience validator if {@code sns.jwt.audience} is configured.</li>
     * </ul>
     * Without these any token signed by the same key — including one minted in another
     * environment that happens to share the keypair — would be accepted.
     */
    @Bean
    public JwtDecoder jwtDecoder(SnsKeySource keys, SnsJwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey())
            .signatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
            .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(props.issuer());
        if (props.audience() == null || props.audience().isBlank()) {
            decoder.setJwtValidator(issuerValidator);
            return decoder;
        }
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            aud -> aud != null && aud.contains(props.audience())
        );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    @Bean
    public ImmutableJWKSet<SecurityContext> jwkSource(SnsKeySource keys) {
        RSAKey rsa = new RSAKey.Builder(keys.publicKey())
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keys.keyId())
            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
            .build();
        return new ImmutableJWKSet<>(new JWKSet(rsa));
    }

    /**
     * {@code /actuator/prometheus} is permitAll-ed only when the request carries the configured
     * scrape bearer token. Otherwise it falls through to the JWT filter (so a real authenticated
     * user can still hit metrics for ad-hoc debugging). When {@code sns.actuator.scrape-token} is
     * blank the matcher returns false → endpoint stays JWT-gated.
     */
    @Bean
    public RequestMatcher prometheusScrapeMatcher(@Value("${sns.actuator.scrape-token:}") String token) {
        byte[] expected = token == null || token.isBlank()
            ? new byte[0]
            : token.getBytes(StandardCharsets.UTF_8);
        return request -> {
            if (expected.length == 0) return false;
            if (!"/actuator/prometheus".equals(request.getRequestURI())) return false;
            String header = request.getHeader("Authorization");
            if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return false;
            byte[] presented = header.substring(7).trim().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, presented);
        };
    }

    /**
     * Maps the {@code role} claim issued by {@link SnsJwtService} to a {@code ROLE_<name>}
     * {@code SimpleGrantedAuthority} so {@code .hasAnyRole("ADMIN","SUPER_ADMIN")} works on the
     * admin matcher. Tokens without a role claim get no admin authority — they remain ordinary
     * authenticated participants for everything outside {@code /api/admin/**}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) return List.of();
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        RequestMatcher prometheusScrapeMatcher,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(
                    "/api/auth/**",
                    "/.well-known/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/ws/**"
                ).permitAll()
                .requestMatchers(prometheusScrapeMatcher).permitAll()
                // /actuator/prometheus without a valid scrape token falls through to JWT auth.
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
