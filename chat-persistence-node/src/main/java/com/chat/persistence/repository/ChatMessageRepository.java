package com.chat.persistence.repository;

import com.chat.persistence.entity.ChatMessageEntity;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatMessageRepository extends ReactiveCassandraRepository<ChatMessageEntity, String> {

    Flux<ChatMessageEntity> findByRoomId(String roomId);
}