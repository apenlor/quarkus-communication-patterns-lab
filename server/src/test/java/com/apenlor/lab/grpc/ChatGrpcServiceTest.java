package com.apenlor.lab.grpc;

import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ChatGrpcServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ChatGrpcServiceTest.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @GrpcClient
    ChatService client;

    @Test
    void testBidiChat() {
        // Create dedicated, thread-safe mailboxes for each client
        LinkedBlockingDeque<ChatMessage> messagesListener1 = new LinkedBlockingDeque<>();
        LinkedBlockingDeque<ChatMessage> messagesListener2 = new LinkedBlockingDeque<>();
        LinkedBlockingDeque<ChatMessage> messagesSender = new LinkedBlockingDeque<>();

        // Create the source streams for messages we will send FROM the clients.
        UnicastProcessor<ChatMessage> sourceListener1 = UnicastProcessor.create();
        UnicastProcessor<ChatMessage> sourceListener2 = UnicastProcessor.create();
        UnicastProcessor<ChatMessage> sourceSender = UnicastProcessor.create();

        // Simulate all three clients connecting to the server.
        // We subscribe their mailboxes to the streams coming FROM the server.
        connectClient("Listener 1", sourceListener1, messagesListener1);
        connectClient("Listener 2", sourceListener2, messagesListener2);
        connectClient("Sender", sourceSender, messagesSender);

        // Send a message
        log.info("Sender is sending a message...");
        String messageToSend = "Hello listeners!";
        ChatMessage message = ChatMessage.newBuilder()
                .setSender("Sender")
                .setMessage(messageToSend)
                .build();
        // Push the message into the sender's source stream.
        sourceSender.onNext(message);

        // Assert that both listener clients receive the message.
        log.info("Waiting for listeners to receive the message...");
        Awaitility.await().atMost(TIMEOUT).until(() -> messagesListener1.size() == 1 && messagesListener2.size() == 1);

        // Verify the contents of the received messages.
        assertEquals(messageToSend, Objects.requireNonNull(messagesListener1.poll()).getMessage());
        log.info("Verified Listener 1 received the message.");

        assertEquals(messageToSend, Objects.requireNonNull(messagesListener2.poll()).getMessage());
        log.info("Verified Listener 2 received the message.");

        // Assert that the sender's mailbox remains empty.
        assertTrue(messagesSender.isEmpty(), "Sender should not receive its own message back.");
        log.info("Verified Sender did not receive its own message.");

        // Complete the client-side streams to signal disconnection.
        sourceListener1.onComplete();
        sourceListener2.onComplete();
        sourceSender.onComplete();
    }


    /**
     * Helper method to encapsulate the logic of connecting a single test client.
     *
     * @param clientName The name of the client for logging purposes.
     * @param source     The source stream for messages FROM this client.
     * @param mailbox    The destination queue for messages TO this client.
     */
    private void connectClient(String clientName, UnicastProcessor<ChatMessage> source, BlockingQueue<ChatMessage> mailbox) {
        log.info("Connecting client: {}", clientName);
        client.bidiChat(Multi.createFrom().publisher(source))
                .subscribe().with(
                        // On a new message from the server, add it to this client's mailbox.
                        mailbox::add,
                        // On a failure signal, log it unless it's the expected shutdown error.
                        failure -> {
                            if (!(failure instanceof StatusRuntimeException)) {
                                log.error("{} stream failed unexpectedly", clientName, failure);
                            }
                        }
                );
    }

}