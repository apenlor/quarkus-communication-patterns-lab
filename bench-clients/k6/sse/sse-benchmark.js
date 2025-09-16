/*global __ENV */
// Directive for static analysis tools like Codacy/ESLint.
// Informs the linter that `__ENV` is an expected global variable provided by the k6 runtime.

import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import sse from 'k6/x/sse';

// --- Custom k6 Metrics ---
const timeToFirstMessage = new Trend('time_to_first_message', true);
const failedConnections = new Counter('failed_connections');
const messagesReceived = new Counter('messages_received');

// Read the target URL from an environment variable passed by the runner script
const targetUrl = __ENV.TARGET_URL;
export const options = {
    stages: [
        { duration: '20s', target: 50 }, // 1. Ramp up from 0 to 50 VUs over 20 seconds.
        { duration: '40s', target: 50 }, // 2. Hold the load of 50 VUs for 40 seconds.
        { duration: '10s', target: 0 },  // 3. Ramp down to 0 VUs over 10 seconds.
    ],
    // `thresholds` defines the pass/fail criteria for the test
    thresholds: {
        'failed_connections': ['count==0'], // The test fails if even one connection error occurs.
        'time_to_first_message': ['p(95)<1500'], // The test fails if the 95th percentile for receiving the first message is over 1.5 seconds.
    },
};

// Main function that each VU will execute
export default async function () {
    if (!targetUrl) {
        failedConnections.add(1);
        console.error("FATAL: TARGET_URL environment variable was not provided to the k6 script.");
        return;
    }

    // We wrap the entire SSE client lifecycle in a Promise
    await new Promise((resolve) => {
        let client;
        try {
            let connectionStartTime;

            // Initiate the SSE connection
            const response = sse.open(targetUrl, {}, function (c) {
                client = c;
                let firstMessage = true;

                client.on('open', function () {
                    connectionStartTime = new Date().getTime();
                });

                client.on('event', function (event) {
                    messagesReceived.add(1);

                    // We calculate and record the true 'time to first message
                    if (firstMessage && connectionStartTime) {
                        const now = new Date().getTime();
                        timeToFirstMessage.add(now - connectionStartTime);
                        firstMessage = false;
                    }
                });

                client.on('error', function (e) {
                    failedConnections.add(1);
                });
            });

            // We check the initial HTTP handshake response.
            if (!check(response, { 'handshake_status_is_200': (r) => r && r.status === 200 })) {
                failedConnections.add(1);
                return resolve();
            }

            // Simulate a user staying on the page
            setTimeout(() => {
                if (client) {
                    client.close();
                }
                resolve();
            }, 20000); // Stay connected for 20 seconds

        } catch (e) {
            console.error(`An unexpected error occurred during setup: ${e}`);
            failedConnections.add(1);
            if (client) {
                client.close();
            }
            resolve(); // Always resolve the promise to prevent a hanging iteration.
        }
    });
}