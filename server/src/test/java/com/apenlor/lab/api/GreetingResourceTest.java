package com.apenlor.lab.api;

import com.apenlor.lab.dto.EchoMessage;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Integration tests for the GreetingResource.
 * The @QuarkusTest annotation handles starting & shutting down the application
 */
@QuarkusTest
class GreetingResourceTest {

    @Test
    void testPingEndpoint() {
        given()
                .when().get("/ping")
                .then()
                .statusCode(200)
                .body(is("pong"));
    }

    @Test
    void testEchoEndpoint() {
        EchoMessage requestPayload = new EchoMessage("Hello, JSON!", null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .when()
                .post("/echo")
                .then()
                .statusCode(200)
                .body("message", is("Hello, JSON!"))
                .body("timestamp", notNullValue());
    }
}