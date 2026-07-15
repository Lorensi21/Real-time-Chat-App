package com.chat.persistence.service;

import com.chat.common.models.ChatMessage;
import com.chat.persistence.entity.ChatMessageEntity;
import com.chat.persistence.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Service;

@Service
public class RedisPersistenceSubscriber {

    private static final Logger log = LoggerFactory.getLogger(RedisPersistenceSubscriber.class);
    private static final String CHANNEL_PATTERN = "chat:room:*";

    private final ReactiveRedisTemplate<String, ChatMessage> redisTemplate;
    private final ChatMessageRepository repository;

    public RedisPersistenceSubscriber(ReactiveRedisTemplate<String, ChatMessage> redisTemplate,
                                      ChatMessageRepository repository) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startIngestionStream() {
        log.info("Starting Redis-to-Cassandra ingestion stream...");

        redisTemplate.listenTo(PatternTopic.of(CHANNEL_PATTERN))
                .map(ReactiveSubscription.Message::getMessage)
                .map(this::mapToEntity)
                .flatMap(entity -> repository.save(entity)
                        .doOnError(e -> log.error("Cassandra write failed for Message ID: {}", entity.getRoomId(), e)))
                .doOnError(e -> log.error("Fatal error in persistence ingestion stream", e))
                .subscribe();
    }

    private ChatMessageEntity mapToEntity(ChatMessage dto) {
        return new ChatMessageEntity(
                dto.roomId(),
                dto.timestamp(),
                dto.messageId(),
                dto.senderId(),
                dto.content(),
                dto.type().name()
        );
    }
}
