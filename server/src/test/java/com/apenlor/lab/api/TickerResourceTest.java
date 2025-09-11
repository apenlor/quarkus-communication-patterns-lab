package com.apenlor.lab.api;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TickerResourceTest {

    @ConfigProperty(name = "quarkus.http.test-port")
    int port;

    private HttpClient client;

    @BeforeEach
    void setup() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterEach
    void teardown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testTickerStreamReceivesEvents() throws Exception {
        List<String> receivedEvents = new CopyOnWriteArrayList<>();
        CompletableFuture<Void> completion = new CompletableFuture<>();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/stream/ticker"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    try (Stream<String> bodyStream = response.body()) {
                        bodyStream
                                .takeWhile(line -> receivedEvents.size() < 3)
                                .filter(line -> line.startsWith("data:"))
                                .forEach(receivedEvents::add);
                    }
                    completion.complete(null);
                })
                .exceptionally(ex -> {
                    completion.completeExceptionally(ex);
                    return null;
                });

        completion.get(5, TimeUnit.SECONDS);

        assertEquals(3, receivedEvents.size(), "Should have collected exactly 3 SSE events.");

        String firstEventPayload = receivedEvents.getFirst();
        assertTrue(
                firstEventPayload.matches("data:\\{\"price\":\\d+\\.\\d+,\"timestamp\":\"[\\d\\-T:Z.]+\"}"),
                "Event payload should be a valid TickerMessage in JSON format. Actual: " + firstEventPayload
        );
    }
}