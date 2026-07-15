package com.chat.connection.redis;

import com.chat.common.models.ChatMessage;
import com.chat.connection.registry.LocalSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);
    private static final String CHANNEL_PATTERN = "chat:room:*";

    private final ReactiveRedisTemplate<String, ChatMessage> redisTemplate;
    private final LocalSessionRegistry registry;

    public RedisMessageSubscriber(ReactiveRedisTemplate<String, ChatMessage> redisTemplate,
                                  LocalSessionRegistry registry) {
        this.redisTemplate = redisTemplate;
        this.registry = registry;
    }

    /**
     * Initializes the reactive subscription to Redis as soon as the Spring context is fully loaded.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void subscribeToChatChannels() {
        log.info("Initializing Redis Pub/Sub listener for pattern: {}", CHANNEL_PATTERN);

        redisTemplate.listenTo(PatternTopic.of(CHANNEL_PATTERN))
                .map(ReactiveSubscription.Message::getMessage) // Fixed method reference
                .doOnNext(message -> {
                    log.debug("Received message from Redis broker: {}", message.messageId());
                    registry.broadcastToLocalRoom(message.roomId(), message);
                })
                .doOnError(e -> log.error("Fatal error in Redis subscription stream", e))
                .subscribe();
    }
}
