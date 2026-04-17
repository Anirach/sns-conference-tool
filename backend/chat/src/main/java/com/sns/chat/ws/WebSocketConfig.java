package com.sns.chat.ws;

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
 * </ul>
 * Multi-instance fan-out uses Redis Pub/Sub via {@link RedisChatRelay}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor jwtInterceptor;

    public WebSocketConfig(StompJwtChannelInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
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
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*"); // CORS is locked at the HTTP layer
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtInterceptor);
    }
}
