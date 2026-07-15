package com.chat.persistence.entity;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("chat_messages")
public class ChatMessageEntity {

    @PrimaryKeyColumn(name = "room_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private String roomId;

    @PrimaryKeyColumn(name = "message_timestamp", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, ordinal = 1)
    private Instant timestamp;

    @Column("message_id")
    private String messageId;

    @Column("sender_id")
    private String senderId;

    @Column("content")
    private String content;

    @Column("message_type")
    private String type;

    // Standard constructor, getters, and setters omitted for brevity.
    // In a production environment, generate these or use Java 17+ Records if supported by your specific Spring Data version.

    public ChatMessageEntity(String roomId, Instant timestamp, String messageId, String senderId, String content, String type) {
        this.roomId = roomId;
        this.timestamp = timestamp;
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.type = type;
    }

    public String getRoomId() { return roomId; }
    public Instant getTimestamp() { return timestamp; }
    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getType() { return type; }
}