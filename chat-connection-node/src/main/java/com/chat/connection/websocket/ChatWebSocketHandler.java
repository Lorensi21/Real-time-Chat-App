package com.chat.connection.websocket;

import com.chat.common.models.ChatMessage;
import com.chat.common.models.MessageType;
import com.chat.connection.redis.GlobalPresenceService;
import com.chat.connection.redis.RedisMessagePublisher;
import com.chat.connection.registry.LocalSessionRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final RedisMessagePublisher redisPublisher;
    private final LocalSessionRegistry sessionRegistry;
    private final GlobalPresenceService presenceService;

    // Maps WebSocket Session ID -> User ID for disconnect tracking
    private final Map<String, String> sessionToUserMap = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                RedisMessagePublisher redisPublisher,
                                LocalSessionRegistry sessionRegistry,
                                GlobalPresenceService presenceService) {
        this.objectMapper = objectMapper;
        this.redisPublisher = redisPublisher;
        this.sessionRegistry = sessionRegistry;
        this.presenceService = presenceService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        Sinks.Many<ChatMessage> outboundSink = sessionRegistry.registerSession(sessionId);

        Mono<Void> outbound = session.send(
                outboundSink.asFlux()
                        .map(message -> {
                            try {
                                return session.textMessage(objectMapper.writeValueAsString(message));
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException("Serialization failed", e);
                            }
                        })
        );

        Mono<Void> inbound = session.receive()
                .map(msg -> msg.getPayloadAsText())
                .flatMap(payload -> {
                    try {
                        return Mono.just(objectMapper.readValue(payload, ChatMessage.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Dropped malformed payload from Session {}: {}", sessionId, payload);
                        return Mono.empty();
                    }
                })
                .doOnNext(message -> processInboundMessage(sessionId, message))
                .doFinally(signalType -> handleDisconnect(sessionId))
                .then();

        return Mono.zip(inbound, outbound).then();
    }

    private void processInboundMessage(String sessionId, ChatMessage message) {
        if (message.type() == MessageType.JOIN) {
            sessionRegistry.joinRoom(sessionId, message.roomId());
            sessionToUserMap.put(sessionId, message.senderId());

            // Fire-and-forget Redis write to establish global online status
            presenceService.markUserOnline(message.senderId()).subscribe();
        }

        redisPublisher.publish(message).subscribe();
    }

    private void handleDisconnect(String sessionId) {
        sessionRegistry.removeSession(sessionId);

        String userId = sessionToUserMap.remove(sessionId);
        if (userId != null) {
            // Fire-and-forget Redis delete to clear the routing entry
            presenceService.markUserOffline(userId).subscribe();
            log.info("Client disconnected. Registry and Global Presence cleared for User ID: {}", userId);
        } else {
            log.warn("Unauthenticated client disconnected. Session ID: {}", sessionId);
        }
    }
}
