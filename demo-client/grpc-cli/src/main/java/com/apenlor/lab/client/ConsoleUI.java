package com.apenlor.lab.client;

import com.apenlor.lab.grpc.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Handles all console input and output for the chat client.
 * <p>
 * This class is the dedicated presentation layer. Its sole responsibility is to
 * interact with the user via the console. It uses a dedicated "output" logger to print
 * user-facing text, allowing for centralized control over all console output.
 */
public class ConsoleUI {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleUI.class);
    private static final Logger output = LoggerFactory.getLogger("output");

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Scanner scanner = new Scanner(System.in);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<String> messageSender;

    /**
     * Registers a callback to be invoked when the user has entered a message to be sent.
     *
     * @param messageSender A consumer that accepts the message string.
     */
    public void setMessageSender(Consumer<String> messageSender) {
        this.messageSender = messageSender;
    }

    /**
     * Prompts the user to enter their name via the console and validates it.
     *
     * @return The non-empty name entered by the user.
     */
    public String promptForName() {
        output.info("Enter your name: ");
        String name = scanner.nextLine();
        if (name == null || name.trim().isEmpty()) {
            output.info("Name cannot be empty. Exiting.");
            System.exit(1);
        }
        return name.trim();
    }

    /**
     * Starts the main console input loop. This is a blocking call that will
     * read from System.in until the session is stopped.
     */
    public void start() {
        running.set(true);
        logger.info("Chat session started. Type 'exit' to quit.");
        while (running.get() && scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            if (messageSender != null && !line.trim().isEmpty()) {
                messageSender.accept(line);
            }
        }
        // This message is displayed if the loop was terminated externally
        if (!running.get()) {
            output.info("Connection closed. Press Enter to exit.");
        }
    }

    /**
     * Stops the console input loop. This method is thread-safe and can be
     * called from the communication layer.
     */
    public void stop() {
        running.set(false);
        // Closing the underlying input stream is necessary to unblock the scanner.hasNextLine() call.
        scanner.close();
    }

    /**
     * Displays a formatted chat message received from the server in the console.
     *
     * @param message The ChatMessage to display.
     */
    public void displayMessage(ChatMessage message) {
        String formattedTimestamp = formatTimestamp(message.getTimestamp());
        output.info("[{}] {}: {}", formattedTimestamp, message.getSender(), message.getMessage());
    }

    /**
     * Formats an ISO 8601 timestamp string into a more readable HH:mm:ss format.
     * Includes a fallback to return the original string if parsing fails.
     *
     * @param isoTimestamp The timestamp string from the server.
     * @return A formatted time string.
     */
    private String formatTimestamp(String isoTimestamp) {
        try {
            Instant instant = Instant.parse(isoTimestamp);
            return TIME_FORMATTER.format(instant);
        } catch (DateTimeParseException e) {
            logger.warn("Could not parse timestamp '{}'. Falling back to original.", isoTimestamp);
            // Fallback to a truncated version of the original string if parsing fails.
            return isoTimestamp.length() > 15 ? isoTimestamp.substring(11, 19) : isoTimestamp;
        }
    }
}