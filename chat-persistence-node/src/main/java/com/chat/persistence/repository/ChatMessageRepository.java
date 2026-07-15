package com.chat.persistence.repository;

import com.chat.persistence.entity.ChatMessageEntity;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatMessageRepository extends ReactiveCassandraRepository<ChatMessageEntity, String> {

    /**
     * Retrieves chat history for a specific room.
     * Cassandra handles the descending sort automatically based on the Clustering Key definition.
     */
    Flux<ChatMessageEntity> findByRoomId(String roomId);
}