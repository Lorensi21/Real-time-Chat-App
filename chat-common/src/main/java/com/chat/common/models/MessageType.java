package com.chat.common.models;

public enum MessageType {
    CHAT,      // Standard user-to-user or channel message
    JOIN,      // User connected / entered room
    LEAVE,     // User disconnected / left room
    ACK        // Server-side delivery confirmation
}
