package com.apenlor.lab.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the gRPC ChatService for bidirectional, real-time communication.
 * This service acts as a central hub for a chat room and implements
 * backend-side sender exclusion to prevent message echoing.
 */
@GrpcService
@Singleton
public class ChatGrpcService extends MutinyChatServiceGrpc.ChatServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ChatGrpcService.class);

    /**
     * We use a ConcurrentHashMap to manage the connections
     * Key: A unique String ID (UUID) for each connected client
     * Value: A BroadcastProcessor, which acts as the entry point for pushing messages TO that specific client.
     * This map represents our "chat room" of active participants.
     */
    private final ConcurrentHashMap<String, BroadcastProcessor<ChatMessage>> activeConnections = new ConcurrentHashMap<>();

    @Override
    public Multi<ChatMessage> bidiChat(Multi<ChatMessage> request) {
        // Generate a unique identifier for the connection
        final String connectionId = UUID.randomUUID().toString();
        // Create a dedicated processor for this client
        final BroadcastProcessor<ChatMessage> clientProcessor = BroadcastProcessor.create();

        activeConnections.put(connectionId, clientProcessor);
        log.info("New client connected with ID: {}. Total clients: {}", connectionId, activeConnections.size());


        // We subscribe to the stream of messages coming FROM this client.
        request
                // The .onTermination() operator is a cleanup mechanism. Called when the stream completes
                // (client disconnects gracefully) or fails (error).
                .onTermination().invoke(() -> {
                    activeConnections.remove(connectionId);
                    log.info("Client disconnected with ID: {}. Total clients: {}", connectionId, activeConnections.size());
                })
                .subscribe().with(
                        // This is the handler for each message received FROM the client.
                        incomingMessage -> {
                            log.info("Message from [{}]: {}", incomingMessage.getSender(), incomingMessage.getMessage());

                            // Enrich the message with a server-side timestamp.
                            ChatMessage broadcastMessage = ChatMessage.newBuilder()
                                    .setSender(incomingMessage.getSender())
                                    .setMessage(incomingMessage.getMessage())
                                    .setTimestamp(Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
                                    .build();

                            // Iterate over all active connections to broadcast the message
                            activeConnections.forEach((id, processor) -> {
                                // only send the message to other clients
                                if (!id.equals(connectionId)) {
                                    // The onNext method pushes the item to the processor's subscribers.
                                    processor.onNext(broadcastMessage);
                                }
                            });
                        },
                        // This is the handler for an error in the client's incoming stream
                        failure -> log.error("Client stream for {} failed: {}", connectionId, failure.getMessage())
                );

        // We return the client's personal processor, converted to a Multi (a reactive stream).
        // This is the "pipe" through which this client will receive messages broadcasted
        // from other clients.
        return Multi.createFrom().publisher(clientProcessor);
    }
}