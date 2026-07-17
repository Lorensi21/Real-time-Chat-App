package com.chat.connection.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SecretKey key;
    private static final long ONE_HOUR_IN_MILLIS = 3600000;

    public AuthController(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/token")
    public Mono<String> generateDevToken(@RequestParam String userId) {
        long nowMillis = System.currentTimeMillis();

        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + ONE_HOUR_IN_MILLIS))
                .signWith(key)
                .compact();

        return Mono.just(token);
    }
}
