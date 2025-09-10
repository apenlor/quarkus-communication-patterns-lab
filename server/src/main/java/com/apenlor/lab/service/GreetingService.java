package com.apenlor.lab.service;

import com.apenlor.lab.dto.EchoMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/**
 * A simple service bean to demonstrate dependency injection.
 * In a real-world application, this is where the core business logic would reside,
 * keeping the resource layer clean and focused on HTTP concerns.
 */
@ApplicationScoped
public class GreetingService {

    /**
     * Provides a static response for the health-check-style ping endpoint.
     *
     * @return The string "pong".
     */
    public String ping() {
        return "pong";
    }

    /**
     * Processes an EchoMessage request and returns a new EchoMessage response.
     * This simulates a more realistic API that works with structured data.
     * It adds a server-side timestamp to the response.
     *
     * @param request The incoming EchoMessage from the client.
     * @return A new EchoMessage containing the original message and a server timestamp.
     */
    public EchoMessage echo(EchoMessage request) {
        return new EchoMessage(request.message(), Instant.now());
    }
}