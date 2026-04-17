package com.sns.identity.security;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(SnsJwtProperties.class)
public class SecurityConfig {

    @Bean
    public SnsKeySource snsKeySource(SnsJwtProperties props) {
        return SnsKeySource.fromProperties(props);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtDecoder jwtDecoder(SnsKeySource keys) {
        return NimbusJwtDecoder.withPublicKey(keys.publicKey())
            .signatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
            .build();
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                    "/actuator/prometheus",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/ws/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));
        return http.build();
    }
}
