package com.sns.chat.ws;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket wiring.
 * <ul>
 *   <li>{@code /ws} is the CONNECT endpoint (SockJS fallback off — mobile and web both support raw WS).</li>
 *   <li>Simple broker handles {@code /topic} (broadcast) and {@code /queue} (user-scoped).</li>
 *   <li>Client-bound messages use the {@code /app} prefix.</li>
 *   <li>JWT is carried in the STOMP CONNECT header; {@link StompJwtChannelInterceptor} authenticates.</li>
 *   <li>Allowed origins come from {@code sns.security.cors.allowed-origins} (CSV) — the same
 *       property that drives the HTTP CORS filter in {@code SnsCorsConfiguration}. An empty list
 *       refuses every cross-origin handshake (same-origin only).</li>
 * </ul>
 * Multi-instance fan-out uses Redis Pub/Sub via {@link RedisChatRelay}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor jwtInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(
        StompJwtChannelInterceptor jwtInterceptor,
        @Value("${sns.security.cors.allowed-origins:}") String corsCsv
    ) {
        this.jwtInterceptor = jwtInterceptor;
        this.allowedOrigins = corsCsv == null || corsCsv.isBlank()
            ? new String[0]
            : Arrays.stream(corsCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[] { 10_000, 10_000 })
            .setTaskScheduler(new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler() {{
                setPoolSize(1);
                setThreadNamePrefix("ws-heartbeat-");
                initialize();
            }});
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var registration = registry.addEndpoint("/ws");
        if (allowedOrigins.length > 0) {
            registration.setAllowedOrigins(allowedOrigins);
        }
        // No setAllowedOriginPatterns("*"): with an empty allow-list, only same-origin handshakes
        // succeed, which is the safe default. Operators opt in by listing concrete URLs in env.
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtInterceptor);
    }
}
