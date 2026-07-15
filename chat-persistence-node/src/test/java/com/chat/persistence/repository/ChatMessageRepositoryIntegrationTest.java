package com.chat.persistence.repository;

import com.chat.persistence.entity.ChatMessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

@DataCassandraTest
@Testcontainers
public class ChatMessageRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:4.1");

    @Autowired
    private ChatMessageRepository repository;

    @Test
    void shouldSaveAndRetrieveMessagesInDescendingChronologicalOrder() {
        String roomId = "system-architecture-room";
        Instant baseTime = Instant.now();

        ChatMessageEntity msg1 = new ChatMessageEntity(
                roomId, baseTime.minusSeconds(10), UUID.randomUUID().toString(), "user-1", "Initial message", "CHAT"
        );
        ChatMessageEntity msg2 = new ChatMessageEntity(
                roomId, baseTime, UUID.randomUUID().toString(), "user-2", "Latest message", "CHAT"
        );

        repository.save(msg1).block();
        repository.save(msg2).block();

        StepVerifier.create(repository.findByRoomId(roomId))
                .expectNextMatches(entity -> entity.getContent().equals("Latest message"))
                .expectNextMatches(entity -> entity.getContent().equals("Initial message"))
                .verifyComplete();
    }
}
