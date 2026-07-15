package com.chat.connection.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class GlobalPresenceService {

    private static final Logger log = LoggerFactory.getLogger(GlobalPresenceService.class);
    private static final String PRESENCE_KEY_PREFIX = "presence:user:";

    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final String nodeId;

    public GlobalPresenceService(ReactiveStringRedisTemplate stringRedisTemplate, String nodeId) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.nodeId = nodeId;
    }

    /**
     * Binds a user to this specific node instance.
     */
    public Mono<Boolean> markUserOnline(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        // A 24-hour TTL acts as a circuit breaker. If the JVM hard-crashes and the disconnect
        // hook fails to execute, the key will eventually expire, preventing permanent zombie states.
        return stringRedisTemplate.opsForValue()
                .set(key, nodeId, Duration.ofHours(24))
                .doOnSuccess(success -> log.debug("User {} marked online on Node {}", userId, nodeId));
    }

    /**
     * Removes the user's routing entry from the global registry.
     */
    public Mono<Long> markUserOffline(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        return stringRedisTemplate.delete(key)
                .doOnSuccess(deleted -> log.debug("User {} marked offline", userId));
    }

    /**
     * Resolves the current connection node for a given user.
     */
    public Mono<String> resolveNodeForUser(String userId) {
        return stringRedisTemplate.opsForValue().get(PRESENCE_KEY_PREFIX + userId);
    }
}