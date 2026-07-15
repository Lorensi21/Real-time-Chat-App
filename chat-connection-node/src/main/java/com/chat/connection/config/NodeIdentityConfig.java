package com.chat.connection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class NodeIdentityConfig {

    @Bean
    public String nodeId() {
        return UUID.randomUUID().toString();
    }
}