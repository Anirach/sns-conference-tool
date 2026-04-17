package com.sns.chat.ws;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP CONNECT frames by extracting the Bearer JWT from the {@code Authorization}
 * header and resolving a {@link JwtAuthenticationToken}. Sets the user principal so server-side
 * fan-out via {@code convertAndSendToUser(userId, ...)} addresses the right session.
 */
@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    public StompJwtChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
            }
            String bearer = authHeaders.get(0);
            if (!bearer.toLowerCase().startsWith("bearer ")) {
                throw new IllegalArgumentException("Authorization must be Bearer");
            }
            String token = bearer.substring(7).trim();
            Jwt jwt = jwtDecoder.decode(token);
            UUID userId;
            try {
                userId = UUID.fromString(jwt.getSubject());
            } catch (Exception e) {
                throw new IllegalArgumentException("JWT subject is not a UUID");
            }
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(), userId.toString());
            accessor.setUser(auth);
        }
        return message;
    }

    /** User principal whose {@code getName()} is the UUID — used by user-destination routing. */
    public record UserIdPrincipal(UUID userId) implements Principal {
        @Override public String getName() { return userId.toString(); }
    }
}
