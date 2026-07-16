package com.chat.connection.websocket;

import com.chat.common.models.ChatMessage;
import com.chat.common.models.MessageType;
import com.chat.connection.redis.GlobalPresenceService;
import com.chat.connection.redis.RedisMessagePublisher;
import com.chat.connection.registry.LocalSessionRegistry;
import com.chat.connection.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
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
    private final JwtService jwtService;
    private final LocalSessionRegistry sessionRegistry;
    private final GlobalPresenceService presenceService;

    private final Map<String, String> sessionToUserMap = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                RedisMessagePublisher redisPublisher,
                                JwtService jwtService,
                                LocalSessionRegistry sessionRegistry,
                                GlobalPresenceService presenceService) {
        this.objectMapper = objectMapper;
        this.redisPublisher = redisPublisher;
        this.jwtService = jwtService;
        this.sessionRegistry = sessionRegistry;
        this.presenceService = presenceService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        String token = extractTokenFromQuery(session.getHandshakeInfo().getUri().getQuery());
        String authenticatedUserId = jwtService.extractUserIdIfValid(token);

        // Security: Validate before allocating registry resources to prevent memory leaks
        if (authenticatedUserId == null) {
            log.warn("Unauthorized connection attempt dropped. Session ID: {}", sessionId);
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        // Safe to allocate Sink now
        Sinks.Many<ChatMessage> outboundSink = sessionRegistry.registerSession(sessionId);

        Mono<Void> outbound = session.send(
                outboundSink.asFlux()
                        .map(message -> {
                            try {
                                return session.textMessage(objectMapper.writeValueAsString(message));
                            } catch (JsonProcessingException e) {
                                // Log rather than throwing to prevent terminating the entire Flux on a single serialization failure
                                log.error("Serialization failed for outbound message on Session {}: {}", sessionId, e.getMessage());
                                throw reactor.core.Exceptions.propagate(e);
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
                // Security: Pass the authenticated ID down the chain, do not trust the payload
                .doOnNext(message -> processInboundMessage(sessionId, authenticatedUserId, message))
                .doFinally(signalType -> handleDisconnect(sessionId))
                .then();

        return Mono.zip(inbound, outbound).then();
    }

    private void processInboundMessage(String sessionId, String authenticatedUserId, ChatMessage message) {
        if (message.type() == MessageType.JOIN) {
            sessionRegistry.joinRoom(sessionId, message.roomId());
            sessionToUserMap.put(sessionId, authenticatedUserId);
            presenceService.markUserOnline(authenticatedUserId).subscribe();
        }

        // Security: Discard the client-provided senderId and inject the verified JWT subject
        ChatMessage securedMessage = new ChatMessage(
                message.messageId(),
                message.roomId(),
                authenticatedUserId, // Forcibly injected
                message.content(),
                message.timestamp(),
                message.type()
        );

        redisPublisher.publish(securedMessage).subscribe();
    }

    private String extractTokenFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }

    private void handleDisconnect(String sessionId) {
        sessionRegistry.removeSession(sessionId);
        String userId = sessionToUserMap.remove(sessionId);

        if (userId != null) {
            presenceService.markUserOffline(userId).subscribe();
            log.info("Client disconnected. Registry and Global Presence cleared for User ID: {}", userId);
        } else {
            // This should rarely hit given the upfront JWT block, but handles edge cases safely
            log.debug("Unauthenticated or transient client disconnected. Session ID: {}", sessionId);
        }
    }
}
