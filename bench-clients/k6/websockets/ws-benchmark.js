/*global __ENV */
// Directive for static analysis tools like Codacy/ESLint.
// Informs the linter that `__ENV` is an expected global variable provided by the k6 runtime.

import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import ws from 'k6/ws';

// --- Custom k6 Metrics ---
// Trend for tracking the time from connection open to receiving the first message.
const timeToFirstMessage = new Trend('time_to_first_message', true);
// Trend for tracking the round-trip time of messages sent by the client.
const messageRTT = new Trend('websocket_message_rtt', true);
// Counter for any connections that fail to establish.
const failedConnections = new Counter('failed_connections');

// --- Test Configuration ---
// Read the target URL from an environment variable passed by the runner script.
const targetUrl = __ENV.TARGET_URL;
export const options = {
    stages: [
        { duration: '20s', target: 50 }, // 1. Ramp up from 0 to 50 VUs over 20 seconds.
        { duration: '40s', target: 50 }, // 2. Hold the load of 50 VUs for 40 seconds.
        { duration: '10s', target: 0 },  // 3. Ramp down to 0 VUs over 10 seconds.
    ],
    thresholds: {
        // The test fails if any connection error occurs.
        'failed_connections': ['count==0'],
        // The test fails if the 95th percentile for receiving the first message is over 1.5 seconds.
        'time_to_first_message': ['p(95)<1500'],
        // The test fails if the 95th percentile for message round-trip time is over 500ms.
        'websocket_message_rtt': ['p(95)<500'],
    },
};

// --- Main k6 Virtual User Function ---
export default function () {
    if (!targetUrl) {
        failedConnections.add(1);
        console.error("FATAL: TARGET_URL environment variable was not provided to the k6 script.");
        return;
    }

    // Establish the WebSocket connection. The third argument is a callback with the socket lifecycle.
    const res = ws.connect(targetUrl, {}, function (socket) {
        let connectionStartTime;

        socket.on('open', () => {
            connectionStartTime = new Date().getTime();
            // Send a simple greeting message upon connection.
            socket.send('Hello from k6!');

            // Set up a recurring timer to send a "ping" message with a timestamp.
            // This allows us to measure round-trip time.
            socket.setInterval(() => {
                const message = `ping ${new Date().getTime()}`;
                socket.send(message);
            }, 5000); // Send a message every 5 seconds
        });

        socket.on('message', (data) => {
            const receivedTime = new Date().getTime();

            // This is the first message of any kind received after opening.
            if (connectionStartTime) {
                timeToFirstMessage.add(receivedTime - connectionStartTime);
                // Set to null to ensure we only record this once per connection.
                connectionStartTime = null;
            }

            // Check if it's one of our ping messages to calculate RTT.
            if (data.startsWith('ping')) {
                const sentTime = parseInt(data.split(' ')[1], 10);
                const rtt = receivedTime - sentTime;
                messageRTT.add(rtt);
            }
        });

        socket.on('error', (e) => {
            failedConnections.add(1);
            console.error(`An unexpected WebSocket error occurred: ${e.error()}`);
        });

        // Keep the connection open for a fixed duration to simulate a user session.
        socket.setTimeout(() => {
            socket.close();
        }, 20000); // 20 seconds
    });

    // Check that the initial HTTP handshake for the WebSocket connection was successful (Status 101).
    check(res, { 'WebSocket handshake successful': (r) => r && r.status === 101 });
    if (!res || res.status !== 101) {
        failedConnections.add(1);
    }
}