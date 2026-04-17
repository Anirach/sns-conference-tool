package com.sns.sns.app;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-provider OAuth2 client config. Values come from environment:
 *   sns.oauth.facebook.client-id / client-secret / scopes / redirect-uri
 *   sns.oauth.linkedin.*
 *
 * Not configured ⇒ link/callback requests return 503 so the web UI can hide the
 * provider button.
 */
@Component
public class SnsOauthConfig {

    public record Client(String clientId, String clientSecret, String redirectUri, String authUri, String tokenUri, String scopes) {
        public boolean isConfigured() { return clientId != null && !clientId.isBlank(); }
    }

    private final Map<String, Client> clients;

    public SnsOauthConfig(
        @Value("${sns.oauth.facebook.client-id:}") String fbClientId,
        @Value("${sns.oauth.facebook.client-secret:}") String fbSecret,
        @Value("${sns.oauth.facebook.redirect-uri:}") String fbRedirect,
        @Value("${sns.oauth.facebook.scopes:public_profile email}") String fbScopes,
        @Value("${sns.oauth.linkedin.client-id:}") String liClientId,
        @Value("${sns.oauth.linkedin.client-secret:}") String liSecret,
        @Value("${sns.oauth.linkedin.redirect-uri:}") String liRedirect,
        @Value("${sns.oauth.linkedin.scopes:openid profile email}") String liScopes
    ) {
        this.clients = Map.of(
            "FACEBOOK", new Client(
                fbClientId, fbSecret, fbRedirect,
                "https://www.facebook.com/v19.0/dialog/oauth",
                "https://graph.facebook.com/v19.0/oauth/access_token",
                fbScopes
            ),
            "LINKEDIN", new Client(
                liClientId, liSecret, liRedirect,
                "https://www.linkedin.com/oauth/v2/authorization",
                "https://www.linkedin.com/oauth/v2/accessToken",
                liScopes
            )
        );
    }

    public Client client(String provider) {
        return clients.get(provider.toUpperCase());
    }
}
