package com.chat.connection.redis;

import com.chat.common.models.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisMessagePublisher.class);
    private static final String CHANNEL_PREFIX = "chat:room:";

    private final ReactiveRedisTemplate<String, ChatMessage> redisTemplate;

    public RedisMessagePublisher(ReactiveRedisTemplate<String, ChatMessage> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Long> publish(ChatMessage message) {
        String topic = CHANNEL_PREFIX + message.roomId();
        return redisTemplate.convertAndSend(topic, message)
                .doOnSuccess(receivers -> log.debug("Message {} published to {} (Receivers: {})",
                        message.messageId(), topic, receivers))
                .doOnError(e -> log.error("Failed to publish message: {}", message.messageId(), e));
    }
}
