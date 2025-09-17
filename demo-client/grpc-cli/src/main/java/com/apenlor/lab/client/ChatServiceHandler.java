package com.apenlor.lab.client;

import com.apenlor.lab.grpc.ChatMessage;
import com.apenlor.lab.grpc.ChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Encapsulates all gRPC communication logic for the chat client.
 * <p>
 * This class is the dedicated communication layer. It manages the lifecycle of the
 * connection (channel), handles the bidirectional streaming, and exposes a clean, high-level
 * API to the rest of the application.
 *
 */
public class ChatServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceHandler.class);

    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;

    // The observer used to send messages TO the server. It is provided by the gRPC framework.
    private StreamObserver<ChatMessage> requestObserver;
    // Callbacks to communicate with the UI layer.
    private Consumer<ChatMessage> messageReceiver;
    private Runnable completionHandler;

    private String senderName;

    public ChatServiceHandler(String host, int port) {
        // The target string uses the "dns" scheme to force the standard DNS name resolver,
        // making the connection robust across different environments.
        String target = "dns:///" + host + ":" + port;
        // A ManagedChannel is the core object representing a connection to a gRPC server.
        this.channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext() // Necessary for labs without TLS configured.
                .build();
        // The "stub" is the client-side proxy for the remote service. We use the async stub for streaming.
        this.asyncStub = ChatServiceGrpc.newStub(channel);
    }

    /**
     * Registers a callback to be invoked when a message is received from the server.
     *
     * @param messageReceiver A consumer that will accept the incoming ChatMessage.
     */
    public void setMessageReceiver(Consumer<ChatMessage> messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    /**
     * Registers a callback to be invoked when the gRPC stream is closed (completed or failed).
     *
     * @param completionHandler A runnable that will be executed on stream closure.
     */
    public void setCompletionHandler(Runnable completionHandler) {
        this.completionHandler = completionHandler;
    }

    /**
     * Connects to the gRPC server and initiates the bidirectional chat stream.
     *
     * @param name The name of the user connecting, to be used as the sender.
     */
    public void connect(String name) {
        this.senderName = name;
        logger.info("Connecting to server as '{}'...", senderName);

        // To handle bidirectional streaming, we create an observer that will process
        // responses FROM the server.
        StreamObserver<ChatMessage> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage message) {
                // This method is called by the gRPC runtime whenever a message is received.
                if (messageReceiver != null) {
                    messageReceiver.accept(message);
                }
            }

            @Override
            public void onError(Throwable t) {
                // This is called when the stream terminates with an error.
                logger.error("gRPC stream failed", t);
                if (completionHandler != null) {
                    completionHandler.run();
                }
            }

            @Override
            public void onCompleted() {
                // This is called when the server gracefully closes the stream.
                logger.info("Server stream completed.");
                if (completionHandler != null) {
                    completionHandler.run();
                }
            }
        };

        // By calling the bidiChat method and passing our responseObserver, we initiate the stream.
        // The gRPC framework returns a requestObserver, which is our handle for sending messages.
        requestObserver = asyncStub.bidiChat(responseObserver);
    }

    /**
     * Sends a chat message to the server.
     *
     * @param messageText The text of the message to send.
     */
    public void sendMessage(String messageText) {
        if (requestObserver == null) {
            logger.warn("Cannot send message, not connected.");
            return;
        }
        ChatMessage request = ChatMessage.newBuilder()
                .setSender(this.senderName)
                .setMessage(messageText)
                .build();
        // We use the requestObserver provided by gRPC to send our message.
        requestObserver.onNext(request);
    }

    /**
     * Disconnects from the server gracefully.
     * This involves signaling completion to the server and shutting down the channel.
     */
    public void disconnect() {
        logger.info("Disconnecting from server...");
        if (requestObserver != null) {
            try {
                // Signal to the server that this client is done sending messages.
                requestObserver.onCompleted();
            } catch (Exception e) {
                logger.warn("Error while completing request stream.", e);
            }
        }
        try {
            if (!channel.isShutdown()) {
                // Initiate a graceful shutdown of the channel.
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted during channel shutdown.", e);
            Thread.currentThread().interrupt(); // Preserve the interrupted status.
        }
        logger.info("Channel shut down.");
    }
}