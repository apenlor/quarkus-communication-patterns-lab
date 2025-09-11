package com.apenlor.lab.dto;

/**
 * DTO representing a single stock ticker update.
 * Using a record for a concise, immutable data carrier.
 *
 * @param price     The random price for the ticker.
 * @param timestamp The ISO-8601 timestamp of when the event was generated.
 */
public record TickerMessage(double price, String timestamp) {
}