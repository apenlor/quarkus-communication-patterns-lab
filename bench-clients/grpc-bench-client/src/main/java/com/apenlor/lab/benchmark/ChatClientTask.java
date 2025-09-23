package com.apenlor.lab.benchmark;

import com.apenlor.lab.grpc.ChatMessage;
import com.apenlor.lab.grpc.ChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single virtual user in the benchmark.
 * Each instance of this task runs on its own thread, managing one gRPC connection.
 */
public class ChatClientTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ChatClientTask.class);

    private final int clientId;
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final Histogram histogram;
    private final CountDownLatch startLatch;
    private final CountDownLatch finishLatch;
    private final AtomicLong timeoutCounter;

    public ChatClientTask(int clientId, String host, int port, Histogram histogram, CountDownLatch startLatch, CountDownLatch finishLatch, AtomicLong timeoutCounter) {
        this.clientId = clientId;
        String target = "dns:///" + host + ":" + port;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.asyncStub = ChatServiceGrpc.newStub(this.channel);
        this.histogram = histogram;
        this.startLatch = startLatch;
        this.finishLatch = finishLatch;
        this.timeoutCounter = timeoutCounter;
    }

    @Override
    public void run() {
        try {
            // A thread-safe queue to receive messages from the server.
            BlockingQueue<ChatMessage> incomingMessages = new LinkedBlockingQueue<>();
            StreamObserver<ChatMessage> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(ChatMessage value) {
                    incomingMessages.add(value);
                }

                @Override
                public void onError(Throwable t) {
                    // Errors are expected during forceful shutdown
                }

                @Override
                public void onCompleted() {
                    // Do nothing
                }
            };

            StreamObserver<ChatMessage> requestObserver = asyncStub.bidiChat(responseObserver);

            // Wait for the main thread to give the "start" signal.
            startLatch.await();

            // Main benchmark loop. It will run until the main thread interrupts it.
            while (!Thread.currentThread().isInterrupted()) {
                long sentTimeNanos = System.nanoTime();
                requestObserver.onNext(ChatMessage.newBuilder()
                        .setSender("client-" + clientId)
                        .setMessage("ping")
                        .build());

                // Block and wait for a message to arrive from the server (broadcast from another client).
                ChatMessage receivedMessage = incomingMessages.poll(10, TimeUnit.SECONDS);

                if (receivedMessage != null) {
                    long latencyNanos = System.nanoTime() - sentTimeNanos;
                    // Record the measurement in our shared, thread-safe histogram.
                    histogram.recordValue(latencyNanos);
                } else {
                    timeoutCounter.incrementAndGet();
                }
                TimeUnit.MILLISECONDS.sleep(10);
            }

        } catch (InterruptedException e) {
            // This is the expected way to exit the loop when the main thread stops us.
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Client {} failed with an unexpected error", clientId, e);
        } finally {
            channel.shutdownNow(); // Forcefully close the connection.
            finishLatch.countDown(); // Signal to the main thread that this task is finished.
        }
    }
}