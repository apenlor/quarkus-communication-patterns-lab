package com.apenlor.lab.ws;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the {@link ChatSocket} WebSocket endpoint.
 *
 * <p>
 * The tests in this class are focused on the "happy path" functionality to validate the
 * primary broadcast mechanism, including sender exclusion. This aligns with the project's
 * mission of comparing communication patterns under normal operating conditions.
 * </p>
 * <p>
 * A production-grade test suite would also include negative test cases for failure modes.
 * These are considered out of scope for this lab.
 * </p>
 */
@QuarkusTest
class ChatSocketTest {

    private static final Logger log = LoggerFactory.getLogger(ChatSocketTest.class);
    private static final int TIMEOUT_SECONDS = 10;

    @Inject
    Vertx vertx;

    WebSocketClient webSocketClient;

    @TestHTTPResource("/ws/chat")
    URI uri;

    @BeforeEach
    void setUp() {
        webSocketClient = vertx.createWebSocketClient();
    }

    @AfterEach
    void tearDown() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    @Test
    void testChatBroadcast() throws ExecutionException, InterruptedException, TimeoutException {
        // Create a dedicated message queue for each listener client
        BlockingQueue<String> messagesClient1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> messagesClient2 = new LinkedBlockingQueue<>();
        BlockingQueue<String> messagesSender = new LinkedBlockingQueue<>();

        // Connect all three clients
        CompletableFuture<WebSocket> client1Future = connectClient("Listener 1");
        CompletableFuture<WebSocket> client2Future = connectClient("Listener 2");
        CompletableFuture<WebSocket> senderFuture = connectClient("Sender");

        WebSocket client1 = client1Future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        WebSocket client2 = client2Future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        WebSocket sender = senderFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Route messages to the correct per-client queue
        client1.textMessageHandler(messagesClient1::add);
        client2.textMessageHandler(messagesClient2::add);
        sender.textMessageHandler(messagesSender::add);

        // Send a message from the sending client
        String messageToSend = "Hello listeners!";
        CompletableFuture<Void> sentFuture = new CompletableFuture<>();
        sender.writeTextMessage(messageToSend, result -> {
            if (result.succeeded()) {
                sentFuture.complete(null);
            } else {
                sentFuture.completeExceptionally(result.cause());
            }
        });

        // Wait for the send operation to complete
        sentFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Verify the two listener clients received the message
        String receivedMsg1 = messagesClient1.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(receivedMsg1, "Client 1 did not receive the message in time");
        assertEquals(messageToSend, receivedMsg1);
        log.info("Verified Listener 1 received the message.");

        String receivedMsg2 = messagesClient2.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(receivedMsg2, "Client 2 did not receive the message in time");
        assertEquals(messageToSend, receivedMsg2);
        log.info("Verified Listener 2 received the message.");

        // Verify the sender's queue is empty
        assertTrue(messagesSender.isEmpty(), "Sender should not receive its own message back.");
        log.info("Verified Sender did not receive its own message.");

        // Clean up connections
        client1.close();
        client2.close();
        sender.close();
    }

    private CompletableFuture<WebSocket> connectClient(String clientName) {
        CompletableFuture<WebSocket> connectFuture = new CompletableFuture<>();
        webSocketClient.connect(uri.getPort(), uri.getHost(), uri.getPath(), result -> {
            if (result.succeeded()) {
                log.info("Test client '{}' connected successfully.", clientName);
                connectFuture.complete(result.result());
            } else {
                log.error("Failed to connect client '{}'", clientName, result.cause());
                connectFuture.completeExceptionally(result.cause());
            }
        });
        return connectFuture;
    }
}