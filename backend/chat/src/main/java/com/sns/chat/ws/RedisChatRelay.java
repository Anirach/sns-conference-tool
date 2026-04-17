package com.sns.chat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.chat.api.dto.ChatDtos;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Multi-pod chat fan-out over Redis Pub/Sub. For each persisted message we publish twice —
 * one copy per recipient (sender included so they get a multi-device echo) on channel
 * {@code ws:chat:{userId}}. Every backend instance subscribes to {@code ws:chat:*} and, when
 * it receives a message for a user it has a local STOMP session for, re-emits via the
 * local {@code SimpMessagingTemplate}. Sessions without a local subscriber drop the
 * message — this is fine because the authoritative copy is already in Postgres and
 * history backfill via {@code GET /api/chat/{eventId}/{otherUserId}?since=...} catches
 * any gaps on reconnect.
 *
 * <p>Enabled by default ({@code sns.chat.relay=redis} with {@code matchIfMissing=true}).
 */
@Component
@ConditionalOnProperty(name = "sns.chat.relay", havingValue = "redis", matchIfMissing = true)
public class RedisChatRelay implements ChatRelay, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisChatRelay.class);
    private static final String CHANNEL_PREFIX = "ws:chat:";

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate stomp;
    private final ObjectMapper mapper;

    public RedisChatRelay(
        StringRedisTemplate redis,
        RedisMessageListenerContainer container,
        SimpMessagingTemplate stomp,
        ObjectMapper mapper
    ) {
        this.redis = redis;
        this.container = container;
        this.stomp = stomp;
        this.mapper = mapper;
    }

    @PostConstruct
    void subscribe() {
        container.addMessageListener(this, new PatternTopic(CHANNEL_PREFIX + "*"));
        log.info("RedisChatRelay subscribed to {}*", CHANNEL_PREFIX);
    }

    @Override
    public void deliver(ChatDtos.ChatMessage message) {
        try {
            String payload = mapper.writeValueAsString(message);
            redis.convertAndSend(CHANNEL_PREFIX + message.toUserId(),   payload);
            redis.convertAndSend(CHANNEL_PREFIX + message.fromUserId(), payload);
        } catch (Exception e) {
            log.warn("RedisChatRelay publish failed: {}", e.toString());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        if (!channel.startsWith(CHANNEL_PREFIX)) return;
        UUID userId;
        try {
            userId = UUID.fromString(channel.substring(CHANNEL_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return;
        }
        try {
            ChatDtos.ChatMessage dto = mapper.readValue(message.getBody(), ChatDtos.ChatMessage.class);
            stomp.convertAndSendToUser(userId.toString(), "/queue/chat", dto);
        } catch (Exception e) {
            log.warn("RedisChatRelay onMessage decode failed: {}", e.toString());
        }
    }
}
