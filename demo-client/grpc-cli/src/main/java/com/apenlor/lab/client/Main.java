package com.apenlor.lab.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for the gRPC command-line chat client.
 * <p>
 * This class follows a clean architectural pattern by acting as an orchestrator.
 * Its responsibilities are strictly limited to:
 * 1. Parsing command-line arguments.
 * 2. Instantiating the core application components (the communication and UI layers).
 * 3. Wiring the components together using callbacks (dependency injection).
 * 4. Managing the application's lifecycle, including a graceful shutdown hook.
 * <p>
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Parse command-line arguments for server host and port, providing sensible defaults.
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9001;

        // Instantiate the two primary components: the network handler and the user interface.
        ChatServiceHandler serviceHandler = new ChatServiceHandler(host, port);
        ConsoleUI consoleUI = new ConsoleUI();

        // The components are decoupled and communicate via callbacks (functional interfaces).
        // a. When the UI captures user input, it passes it to the service handler to be sent.
        consoleUI.setMessageSender(serviceHandler::sendMessage);
        // b. When the service handler receives a message, it passes it to the UI to be displayed.
        serviceHandler.setMessageReceiver(consoleUI::displayMessage);
        // c. When the service handler detects a disconnection, it tells the UI to stop its input loop.
        serviceHandler.setCompletionHandler(consoleUI::stop);

        // Register a JVM shutdown hook. This ensures that if the user presses Ctrl+C,
        // our disconnect() logic is called, allowing for a graceful shutdown of the gRPC channel.
        Runtime.getRuntime().addShutdownHook(new Thread(serviceHandler::disconnect));

        // The application starts by connecting to the server with the user-provided name,
        // then enters the blocking UI input loop.
        serviceHandler.connect(consoleUI.promptForName());
        consoleUI.start(); // This call blocks until the chat session ends.

        logger.info("Application has shut down gracefully.");
    }
}