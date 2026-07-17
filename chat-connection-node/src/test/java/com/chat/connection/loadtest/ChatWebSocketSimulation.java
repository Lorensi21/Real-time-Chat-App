package com.chat.connection.loadtest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ChatWebSocketSimulation extends Simulation {

    // 1. Setup the Protocol (Targets your local connection node)
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8081")
            .wsBaseUrl("ws://localhost:8081");

    // 2. Generate unique identities for each virtual user
    private final Iterator<Map<String, Object>> userFeeder = Stream.generate((Supplier<Map<String, Object>>) () ->
            Map.of(
                    "userId", "gatling-" + UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString()
            )
    ).iterator();

    // 3. Define the Virtual User Scenario
    private final ScenarioBuilder scn = scenario("WebSocket Chat Benchmark")
            .feed(userFeeder)
            // Step 1: Authenticate via REST endpoint to receive a valid JWT
            .exec(http("Request JWT Token")
                    .get("/api/auth/token?userId=#{userId}")
                    .check(status().is(200))
                    .check(bodyString().saveAs("jwtToken"))
            )
            .pause(Duration.ofMillis(100))

            // Step 2: Establish Secure WebSocket Handshake
            .exec(ws("Connect WebSocket")
                    .connect("/chat?token=#{jwtToken}")
            )
            .pause(Duration.ofMillis(500))

            // Step 3: Broadcast JOIN presence
            .exec(ws("Send JOIN Message")
                    .sendText("{" +
                            "\"messageId\":\"join-#{userId}\"," +
                            "\"roomId\":\"benchmark-room\"," +
                            "\"senderId\":\"#{userId}\"," +
                            "\"content\":\"Joining benchmark\"," +
                            "\"timestamp\":\"#{timestamp}\"," +
                            "\"type\":\"JOIN\"" +
                            "}")
            )
            .pause(Duration.ofMillis(500))

            // Step 4: Rapid-fire message broadcasting
            .repeat(10, "messageIndex").on(
                    exec(ws("Broadcast CHAT Message")
                            .sendText("{" +
                                    "\"messageId\":\"msg-#{userId}-#{messageIndex}\"," +
                                    "\"roomId\":\"benchmark-room\"," +
                                    "\"senderId\":\"#{userId}\"," +
                                    "\"content\":\"Benchmark payload #{messageIndex}\"," +
                                    "\"timestamp\":\"#{timestamp}\"," +
                                    "\"type\":\"CHAT\"" +
                                    "}")
                    )
                            .pause(Duration.ofMillis(200))
            )

            // Step 5: Graceful Disconnect
            .exec(ws("Close WebSocket").close());

    // 4. Inject Load (Gradually ramp up to 1,000 concurrent users over 15 seconds)
    public ChatWebSocketSimulation() {
        setUp(
                scn.injectOpen(rampUsers(1000).during(Duration.ofSeconds(15)))
        ).protocols(httpProtocol);
    }
}