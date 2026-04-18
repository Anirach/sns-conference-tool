package com.sns.chat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.chat.api.dto.ChatDtos;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Multi-pod chat fan-out over Redis Pub/Sub.
 * <p>
 * Previous version subscribed to {@code ws:chat:*} via {@code PSUBSCRIBE}. That forces Redis to
 * match every single publish against every subscribed pattern across the cluster; with N users
 * publishing and M backend pods each pattern-subscribed, the server pays O(NM) pattern checks
 * per second.
 * <p>
 * New topology:
 * <ul>
 *   <li>Publish: {@code ws:chat:bucket:{userId.hash % BUCKETS}} — still user-addressable, now
 *       collapsed into a fixed channel set (default 64 buckets).</li>
 *   <li>Subscribe: every backend pod subscribes to ALL buckets with plain
 *       {@code SUBSCRIBE}. Redis dispatches by exact-channel match (O(1) per publish).</li>
 *   <li>The published payload still carries the recipient userId so the receiving pod can
 *       call {@code convertAndSendToUser}.</li>
 * </ul>
 * Trade-off: pods now receive every chat message in the system, not just those for locally-
 * connected users. They discard messages with no local session. Cost is a small CPU bump per
 * pod in exchange for eliminating the Redis-server pattern-matching hot spot.
 */
@Component
@ConditionalOnProperty(name = "sns.chat.relay", havingValue = "redis", matchIfMissing = true)
public class RedisChatRelay implements ChatRelay, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisChatRelay.class);
    private static final String CHANNEL_PREFIX = "ws:chat:bucket:";

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate stomp;
    private final ObjectMapper mapper;
    private final int buckets;

    public RedisChatRelay(
        StringRedisTemplate redis,
        RedisMessageListenerContainer container,
        SimpMessagingTemplate stomp,
        ObjectMapper mapper,
        @Value("${sns.chat.relay-buckets:64}") int buckets
    ) {
        this.redis = redis;
        this.container = container;
        this.stomp = stomp;
        this.mapper = mapper;
        this.buckets = Math.max(1, buckets);
    }

    @PostConstruct
    void subscribe() {
        // Subscribe via plain SUBSCRIBE (one channel per bucket) rather than PSUBSCRIBE on a
        // wildcard — Redis dispatch is O(1) per publish with exact-match channels.
        for (int i = 0; i < buckets; i++) {
            container.addMessageListener(this, new ChannelTopic(CHANNEL_PREFIX + i));
        }
        log.info("RedisChatRelay subscribed to {} exact buckets (no PSUBSCRIBE)", buckets);
    }

    @Override
    public void deliver(ChatDtos.ChatMessage message) {
        try {
            String payload = mapper.writeValueAsString(message);
            // Publish once to the recipient's bucket. The receiving pod will dispatch to both
            // toUserId and fromUserId over its local SimpMessagingTemplate (no-op on pods with
            // no matching session), so the sender still gets a multi-device echo without us
            // doubling Redis traffic.
            redis.convertAndSend(channelFor(message.toUserId()), payload);
        } catch (Exception e) {
            log.warn("RedisChatRelay publish failed: {}", e.toString());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatDtos.ChatMessage dto = mapper.readValue(message.getBody(), ChatDtos.ChatMessage.class);
            // Payload carries the intended recipient; STOMP routes it to the session by principal.
            // Pods without a matching local session drop it (cheap no-op).
            stomp.convertAndSendToUser(dto.toUserId().toString(),   "/queue/chat", dto);
            if (!dto.toUserId().equals(dto.fromUserId())) {
                stomp.convertAndSendToUser(dto.fromUserId().toString(), "/queue/chat", dto);
            }
        } catch (Exception e) {
            log.warn("RedisChatRelay onMessage decode failed: {}", e.toString());
        }
    }

    private String channelFor(UUID userId) {
        int bucket = Math.floorMod(userId.hashCode(), buckets);
        return CHANNEL_PREFIX + bucket;
    }
}
