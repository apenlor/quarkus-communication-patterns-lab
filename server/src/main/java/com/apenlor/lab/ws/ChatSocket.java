package com.apenlor.lab.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/chat")
@ApplicationScoped
public class ChatSocket {

    private static final Logger log = LoggerFactory.getLogger(ChatSocket.class);

    // A thread-safe set to store all active sessions.
    private final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Called when a new WebSocket connection is established.
     * Adds the new session to the set of active sessions.
     *
     * @param session The WebSocket session representing the new connection.
     */
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        log.info("New WebSocket session opened: id={}, total sessions={}", session.getId(), sessions.size());
    }

    /**
     * Called when a WebSocket connection is closed.
     * Removes the session from the set of active sessions.
     *
     * @param session The session that is being closed.
     */
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        log.info("WebSocket session closed: id={}, total sessions={}", session.getId(), sessions.size());
    }

    /**
     * Called when a WebSocket error occurs.
     * Logs the error and closes the connection by invoking onClose.
     *
     * @param session   The session where the error occurred.
     * @param throwable The throwable representing the error.
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error on session id={}: {}", session.getId(), throwable.getMessage(), throwable);
        // It's good practice to ensure the session is removed on error.
        sessions.remove(session);
    }

    /**
     * Called when a text message is received from a client.
     * This method broadcasts the received message to all connected sessions
     * *except for the original sender*.
     * <p>
     * For the purpose of this demonstration and benchmark, we are using a simple
     * broadcast model (one-to-all). This allows us to measure the raw performance of the
     * WebSocket transport and the server's fan-out capabilities under load.
     * </p>
     * <p>
     * A production-grade chat application would implement a secure, sophisticated routing mechanism.
     * This would typically involve:
     * <ol>
     *   <li><b>Authentication:</b> Verifying a user's identity during the WebSocket handshake. This is
     *       often done by validating a JWT or session token sent as a query parameter or a special
     *       initial "auth" message immediately after connection. Unauthenticated connections would be rejected.</li>
     *   <li><b>User-Session Mapping:</b> Once authenticated, associating the secure user principal with their
     *       WebSocket session in a thread-safe map.</li>
     *   <li><b>Structured Payloads:</b> Using a format like JSON to include message metadata.</li>
     *   <li><b>Targeted Messaging:</b> The "onMessage" handler would parse this payload and route the
     *       message only to the specified recipient's session, enforcing authorization rules.</li>
     * </ol>
     * </p>
     *
     * @param message The message received from the client.
     * @param session The session from which the message was sent.
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("Message from session id={}: '{}'. Broadcasting to other clients.", session.getId(), message);
        // Pass the original sender's session to the broadcast method for filtering.
        broadcast(message, session);
    }

    /**
     * Helper method to send a message to all active WebSocket sessions,
     * excluding the original sender.
     *
     * @param message The message to be broadcast.
     * @param sender  The session of the client that sent the message.
     */
    private void broadcast(String message, Session sender) {
        sessions.forEach(session -> {
            if (session.isOpen() && !session.getId().equals(sender.getId())) {
                session.getAsyncRemote().sendText(message, result -> {
                    if (result.isOK()) {
                        log.trace("Message sent successfully to session id={}", session.getId());
                    } else {
                        log.error("Failed to send message to session id={}", session.getId(), result.getException());
                    }
                });
            }
        });
    }
}