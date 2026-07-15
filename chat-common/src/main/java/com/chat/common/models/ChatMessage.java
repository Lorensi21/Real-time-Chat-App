package com.chat.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ChatMessage(
        @JsonProperty("messageId") String messageId,
        @JsonProperty("roomId") String roomId,
        @JsonProperty("senderId") String senderId,
        @JsonProperty("content") String content,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("type") MessageType type
) {
    public ChatMessage {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId cannot be null or empty");
        }
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId cannot be null or empty");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
