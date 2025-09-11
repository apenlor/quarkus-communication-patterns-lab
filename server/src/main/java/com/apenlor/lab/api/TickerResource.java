package com.apenlor.lab.api;

import com.apenlor.lab.dto.TickerMessage;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Path("/stream")
public class TickerResource {

    private final Random random = new Random();

    /**
     * Endpoint that streams a new TickerMessage every second.
     * Quarkus RESTEasy Reactive automatically handles serializing the message
     * objects into JSON and formatting them as valid Server-Sent Events.
     *
     * @return A Multi (reactive stream) of TickerMessage objects.
     */
    @GET
    @Path("/ticker")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<TickerMessage> streamTicker() {
        // Generate a new event every second
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .map(tick -> {
                    // Generate a random price
                    double price = 100 + random.nextDouble() * 10;
                    String timestamp = Instant.now().toString();
                    return new TickerMessage(price, timestamp);
                });
    }
}