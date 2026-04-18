package com.sns.app.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Single source of truth for allowed CORS origins. Both the HTTP filter chain (via Spring
 * Security's {@code .cors()} customisation) and the STOMP endpoint pull this list from
 * {@code sns.security.cors.allowed-origins} (CSV).
 *
 * <p>Default is empty — callers must opt in. In dev/prod the operator sets the env var to
 * something concrete (e.g. {@code https://app.sns.example.com,http://localhost:3000} for staging).
 * An empty list locks every cross-origin request out at preflight.
 */
@Configuration
public class SnsCorsConfiguration {

    private final List<String> allowedOrigins;

    public SnsCorsConfiguration(@Value("${sns.security.cors.allowed-origins:}") String csv) {
        this.allowedOrigins = csv == null || csv.isBlank()
            ? List.of()
            : Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        cfg.setExposedHeaders(List.of("X-Request-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        source.registerCorsConfiguration("/.well-known/**", cfg);
        return source;
    }
}
