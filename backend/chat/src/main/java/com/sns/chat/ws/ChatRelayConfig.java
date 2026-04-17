package com.sns.chat.ws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ChatRelayConfig {

    /** Shared listener container for the Redis relay; only created when Redis mode is on. */
    @Bean(destroyMethod = "destroy")
    @ConditionalOnProperty(name = "sns.chat.relay", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory cf) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        return container;
    }
}
