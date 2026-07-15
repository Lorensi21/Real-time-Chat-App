package com.chat.persistence.service;

import com.chat.common.models.ChatMessage;
import com.chat.common.models.MessageType;
import com.chat.persistence.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatHistoryService {

    private final ChatMessageRepository repository;

    public ChatHistoryService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public Flux<ChatMessage> getHistoryByRoomId(String roomId, int limit) {
        // Enforce upper bounds on data retrieval to prevent OOM exceptions from malicious or malformed large-limit requests.
        int safeLimit = Math.min(limit, 100);

        return repository.findByRoomId(roomId)
                .take(safeLimit)
                .map(entity -> new ChatMessage(
                        entity.getMessageId(),
                        entity.getRoomId(),
                        entity.getSenderId(),
                        entity.getContent(),
                        entity.getTimestamp(),
                        MessageType.valueOf(entity.getType())
                ));
    }
}