package com.chat.connection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;

@SpringBootApplication(exclude = { CassandraDataAutoConfiguration.class })
public class ConnectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConnectionApplication.class, args);
    }
}