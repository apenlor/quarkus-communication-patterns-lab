package com.apenlor.lab.api;

import com.apenlor.lab.dto.EchoMessage;
import com.apenlor.lab.service.GreetingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * This class is the entry point for REST requests.
 */
@Path("/")
public class GreetingResource {

    private final GreetingService service;

    @Inject
    public GreetingResource(GreetingService service) {
        this.service = service;
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return service.ping();
    }

    /**
     * Echoes a structured JSON message.
     * This endpoint now consumes and produces JSON, representing a more realistic API contract.
     *
     * @param request The EchoMessage object deserialized from the request body.
     * @return An EchoMessage object which will be serialized to JSON.
     */
    @POST
    @Path("/echo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EchoMessage echo(EchoMessage request) {
        return service.echo(request);
    }
}