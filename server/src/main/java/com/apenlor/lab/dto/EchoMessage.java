package com.apenlor.lab.dto;

import java.time.Instant;

/**
 * A Data Transfer Object (DTO) representing the message for the /echo endpoint.
 *
 * @param message   The text content of the message.
 * @param timestamp The time the message was processed by the server.
 */
public record EchoMessage(String message, Instant timestamp) {
}