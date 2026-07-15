package com.chat.persistence.repository;

import com.chat.persistence.entity.ChatMessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
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

        // Create test entities. Note the timestamps: msg1 is strictly older than msg2.
        ChatMessageEntity msg1 = new ChatMessageEntity(
                roomId, baseTime.minusSeconds(10), UUID.randomUUID().toString(), "user-1", "Initial message", "CHAT"
        );
        ChatMessageEntity msg2 = new ChatMessageEntity(
                roomId, baseTime, UUID.randomUUID().toString(), "user-2", "Latest message", "CHAT"
        );

        // Execute blocking saves purely to seed the database predictably before testing the retrieval stream
        repository.save(msg1).block();
        repository.save(msg2).block();

        // Use StepVerifier to assert the reactive Flux stream
        StepVerifier.create(repository.findByRoomId(roomId))
                // The most recent message (msg2) must be yielded first due to our CLUSTERED ordering definition
                .expectNextMatches(entity -> entity.getContent().equals("Latest message"))
                .expectNextMatches(entity -> entity.getContent().equals("Initial message"))
                .verifyComplete();
    }
}
