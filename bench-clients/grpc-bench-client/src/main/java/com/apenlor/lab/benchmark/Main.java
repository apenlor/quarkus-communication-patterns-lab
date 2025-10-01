package com.apenlor.lab.benchmark;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
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
        // Use a dedicated method to parse arguments for better structure and readability
        final BenchmarkConfig config = parseArgs(args);
        // If parsing fails, parseArgs will print the usage info and return null.
        if (config == null) {
            System.exit(1);
        }

        // All real-time progress logging is conditional
        // This ensures a clean output when the --quiet flag is used, suitable for automated log parsing
        if (!config.quietMode) {
            logger.info("Starting gRPC Benchmark with configuration:");
            logger.info("Target: {}:{}", config.host, config.port);
            logger.info("Concurrency (Virtual Users): {}", config.concurrency);
            logger.info("Duration: {} seconds", config.durationSeconds);
            logger.info("--------------------------------------------------");
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(config.concurrency)) {
            final Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
            final AtomicLong timeoutCounter = new AtomicLong(0);

            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch finishLatch = new CountDownLatch(config.concurrency);

            // Create and submit a task for each virtual user.
            for (int i = 0; i < config.concurrency; i++) {
                // Pass the full config, though the task only needs a subset
                // In a larger application, a dedicated task-specific config might be passed
                Runnable task = new ChatClientTask(i, config.host, config.port, histogram, startLatch, finishLatch, timeoutCounter);
                executor.submit(task);
            }

            if (!config.quietMode) {
                logger.info("All virtual users initialized. Starting benchmark in 3 seconds...");
            }
            Thread.sleep(3000);

            if (!config.quietMode) {
                logger.info("GO!");
            }
            startLatch.countDown(); // This releases all waiting client threads simultaneously.

            Thread.sleep(TimeUnit.SECONDS.toMillis(config.durationSeconds));

            if (!config.quietMode) {
                logger.info("Time's up. Requesting client shutdown...");
            }
            executor.shutdownNow(); // This interrupts the client threads, signaling them to stop

            // Wait for all threads to confirm clean shutdown via the finishLatch.
            if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                if (!config.quietMode) {
                    logger.warn("Benchmark did not complete cleanly. {} tasks did not finish.", finishLatch.getCount());
                }
            } else {
                if (!config.quietMode) {
                    logger.info("All client tasks finished cleanly.");
                }
            }

            printResults(histogram, timeoutCounter, config.durationSeconds, config.quietMode);
        }
    }

    /**
     * Parses command-line arguments into a structured config object.
     * This approach is more robust and extensible than simple array index access.
     * It supports a '--quiet' flag for suppressing verbose output.
     *
     * @param args The command-line arguments provided at runtime.
     * @return A populated BenchmarkConfig object, or null if essential arguments are missing.
     */
    private static BenchmarkConfig parseArgs(String[] args) {
        BenchmarkConfig config = new BenchmarkConfig();
        int positionalArgIndex = 0;

        // Iterate through all provided arguments.
        for (String arg : args) {
            if ("--quiet".equals(arg)) {
                // If it's the quiet flag, set the boolean.
                config.quietMode = true;
            } else if (!arg.startsWith("--")) {
                // If it's a positional argument, assign it based on its order.
                switch (positionalArgIndex) {
                    case 0:
                        config.host = arg;
                        break;
                    case 1:
                        config.port = Integer.parseInt(arg);
                        break;
                    case 2:
                        config.concurrency = Integer.parseInt(arg);
                        break;
                    case 3:
                        config.durationSeconds = Integer.parseInt(arg);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + positionalArgIndex);
                }
                positionalArgIndex++;
            }
        }

        if (positionalArgIndex < 4) {
            // Print usage directly to System.out to ensure it's visible regardless of logger configuration.
            System.out.println("Usage: java -jar <jar_file> [--quiet] <host> <port> <concurrency> <duration_seconds>");
            return null;
        }
        return config;
    }

    /**
     * Prints a formatted summary of the benchmark results.
     * Now supports a quiet mode to print directly to System.out, ensuring the
     * final report is captured by automation scripts without logger noise.
     *
     * @param histogram      The histogram containing all collected latency measurements.
     * @param timeoutCounter The counter for back-pressure events.
     * @param duration       The total duration of the test in seconds.
     * @param quietMode      If true, prints to System.out; otherwise, uses the SLF4J logger.
     */
    private static void printResults(Histogram histogram, AtomicLong timeoutCounter, int duration, boolean quietMode) {
        // Determine the output stream. In quiet mode, we bypass the logger to guarantee the report is the ONLY
        // thing printed to standard output.
        PrintStream out = quietMode ? System.out : null;

        if (histogram.getTotalCount() == 0) {
            logOrPrint(out, "No measurements were recorded. This might indicate a connection or logic issue.");
            return;
        }

        double meanMicros = TimeUnit.NANOSECONDS.toMicros((long) histogram.getMean());
        String formattedMean = String.format("%.2f", meanMicros);

        double messagesPerSecond = (histogram.getTotalCount() / (double) duration);
        String formattedThroughput = String.format("%.2f", messagesPerSecond);

        logOrPrint(out, "-------------------- Benchmark Results --------------------");
        logOrPrint(out, "Total Messages Measured: {}", histogram.getTotalCount());
        logOrPrint(out, "Total Timeouts: {} (indicates back-pressure)", timeoutCounter.get());
        logOrPrint(out, "Throughput: {} msg/sec", formattedThroughput);
        logOrPrint(out, "---------------------------------------------------------");
        logOrPrint(out, "Latency (microseconds):");
        logOrPrint(out, "  min:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getMinValue()));
        logOrPrint(out, "  mean:     {}", formattedMean);
        logOrPrint(out, "  p50 (median): {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(50)));
        logOrPrint(out, "  p90:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(90)));
        logOrPrint(out, "  p99:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(99)));
        logOrPrint(out, "  p99.9:    {}", TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(99.9)));
        logOrPrint(out, "  max:      {}", TimeUnit.NANOSECONDS.toMicros(histogram.getMaxValue()));
        logOrPrint(out, "---------------------------------------------------------");
    }

    /**
     * A helper utility to direct output either to the SLF4J logger or a PrintStream.
     * This avoids code duplication in the printResults method.
     *
     * @param out    The PrintStream to use. If null, the SLF4J logger is used instead.
     * @param format The message format string, using SLF4J's '{}' placeholder style.
     * @param args   The arguments to be formatted into the message.
     */
    private static void logOrPrint(java.io.PrintStream out, String format, Object... args) {
        if (out != null) {
            String printfFormat = format.replace("{}", "%s") + "%n";
            out.printf(printfFormat, args);
        } else {
            // If the PrintStream is null, we fall back to our standard logger.
            logger.info(format, args);
        }
    }

    /**
     * A simple, private static inner class to hold parsed command-line arguments.
     * This is a clean alternative to passing multiple primitive variables through methods.
     */
    private static class BenchmarkConfig {
        String host;
        int port;
        int concurrency;
        int durationSeconds;
        boolean quietMode = false; // Defaults to verbose logging
    }
}