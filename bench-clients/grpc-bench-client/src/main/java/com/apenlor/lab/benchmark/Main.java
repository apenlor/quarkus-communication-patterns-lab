package com.apenlor.lab.benchmark;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The main entry point for the custom gRPC benchmark client.
 * <p>
 * This application simulates a configurable number of concurrent clients to
 * load test the gRPC BidiChat service and measure end-to-end broadcast latency.
 * It follows a robust multithreaded benchmark pattern using CountDownLatches
 * for synchronization and HdrHistogram for accurate, low-overhead measurement.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        // Argument parsing
        if (args.length < 4) {
            logger.info("Usage: java -jar <jar_file> <host> <port> <concurrency> <duration_seconds>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int concurrency = Integer.parseInt(args[2]);
        int durationSeconds = Integer.parseInt(args[3]);

        logger.info("Starting gRPC Benchmark with configuration:");
        logger.info("Target: {}:{}", host, port);
        logger.info("Concurrency (Virtual Users): {}", concurrency);
        logger.info("Duration: {} seconds", durationSeconds);
        logger.info("--------------------------------------------------");

        // Resource initialization
        try (ExecutorService executor = Executors.newFixedThreadPool(concurrency)) {
            // A high-fidelity, thread-safe histogram for aggregating latency measurements.
            // It's configured to record values up to 10 seconds with 3 significant decimal points.
            final Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
            final AtomicLong timeoutCounter = new AtomicLong(0);

            // Latches are used to synchronize the threads for a controlled start and finish
            final CountDownLatch startLatch = new CountDownLatch(1); // All threads wait on this before starting.
            final CountDownLatch finishLatch = new CountDownLatch(concurrency); // Main thread waits on this for all tasks to end

            // Create and submit a task for each virtual user to the thread pool.
            for (int i = 0; i < concurrency; i++) {
                Runnable task = new ChatClientTask(i, host, port, histogram, startLatch, finishLatch, timeoutCounter);
                executor.submit(task);
            }

            // Benchmark execution
            logger.info("All virtual users initialized. Starting benchmark in 3 seconds...");
            Thread.sleep(3000); // A pause to allow all connections to establish

            logger.info("GO!");
            startLatch.countDown(); // Releases all waiting threads

            // The main thread sleeps for the duration of the test.
            Thread.sleep(TimeUnit.SECONDS.toMillis(durationSeconds));

            logger.info("Time's up. Requesting client shutdown...");
            // Interrupting the threads is the signal for them to exit their main loop.
            executor.shutdownNow();

            // Wait for all client tasks to call finishLatch.countDown() in their finally blocks.
            if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                logger.warn("Benchmark did not complete cleanly. {} tasks did not finish.", finishLatch.getCount());
            } else {
                logger.info("All client tasks finished cleanly.");
            }
            // Once all tasks are finished, print the aggregated results from the histogram.
            printResults(histogram, timeoutCounter, durationSeconds);
        }
    }

    /**
     * Prints a formatted summary of the benchmark results from the HdrHistogram.
     *
     * @param histogram The histogram containing all collected latency measurements.
     */
    private static void printResults(Histogram histogram, java.util.concurrent.atomic.AtomicLong timeoutCounter, int duration) {
        logger.info("-------------------- Benchmark Results --------------------");
        if (histogram.getTotalCount() == 0) {
            logger.info("No measurements were recorded. This might indicate a connection or logic issue.");
            return;
        }

        double meanMicros = TimeUnit.NANOSECONDS.toMicros((long) histogram.getMean());
        String formattedMean = String.format("%.2f", meanMicros);

        double messagesPerSecond = (histogram.getTotalCount() / (double) duration);
        String formattedThroughput = String.format("%.2f", messagesPerSecond);

        logger.info("Total Messages Measured: {}", histogram.getTotalCount());
        logger.info("Total Timeouts: {} (indicates back-pressure)", timeoutCounter.get());
        logger.info("Throughput: {} msg/sec", formattedThroughput);
        logger.info("---------------------------------------------------------");
        logger.info("Latency (microseconds):");
        logger.info("  min:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getMinValue()));
        logger.info("  mean:     {}", formattedMean);
        logger.info("  p50 (median): {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(50)));
        logger.info("  p90:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(90)));
        logger.info("  p99:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(99)));
        logger.info("  p99.9:    {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(99.9)));
        logger.info("  max:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getMaxValue()));
        logger.info("---------------------------------------------------------");
    }
}