package com.chat.connection.registry;

import com.chat.common.models.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class LocalSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(LocalSessionRegistry.class);

    // Maps Session ID -> Client's Reactive Outbound Sink
    private final Map<String, Sinks.Many<ChatMessage>> sessionSinks = new ConcurrentHashMap<>();

    // Maps Room ID -> Set of Session IDs currently in that room
    private final Map<String, Set<String>> roomDirectory = new ConcurrentHashMap<>();

    /**
     * Registers a new client connection and provides its outbound sink.
     */
    public Sinks.Many<ChatMessage> registerSession(String sessionId) {
        // unicast() ensures only one subscriber (the WebSocket outbound stream) reads from this sink.
        Sinks.Many<ChatMessage> sink = Sinks.many().unicast().onBackpressureBuffer();
        sessionSinks.put(sessionId, sink);
        return sink;
    }

    /**
     * Maps a session to a specific room for message routing.
     */
    public void joinRoom(String sessionId, String roomId) {
        roomDirectory.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        log.debug("Session {} joined Room {}", sessionId, roomId);
    }

    /**
     * Pushes a message to all local sessions subscribed to the target room.
     */
    public void broadcastToLocalRoom(String roomId, ChatMessage message) {
        Set<String> sessionIds = roomDirectory.getOrDefault(roomId, Set.of());
        for (String sessionId : sessionIds) {
            Sinks.Many<ChatMessage> sink = sessionSinks.get(sessionId);
            if (sink != null) {
                // Emit failure is safely ignored here to prevent one slow client from crashing the routing loop.
                sink.tryEmitNext(message);
            }
        }
    }

    /**
     * Cleans up all references to prevent memory leaks on disconnect.
     */
    public void removeSession(String sessionId) {
        sessionSinks.remove(sessionId);
        roomDirectory.values().forEach(sessions -> sessions.remove(sessionId));
        log.debug("Cleaned up registry for Session {}", sessionId);
    }
}