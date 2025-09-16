/*global __ENV */
// Informs the linter that `__ENV` is a global variable provided by the k6 runtime.

import { check } from 'k6';
import http from 'k6/http';
import { Counter } from 'k6/metrics';

// --- Custom k6 Metrics ---
const failedRequests = new Counter('failed_requests');

// --- Test Configuration ---
// Read the target URL from an environment variable passed by the runner script.
const targetUrl = __ENV.TARGET_URL;
export const options = {
    stages: [
        { duration: '20s', target: 100 }, // 1. Ramp up to 100 concurrent VUs over 20 seconds.
        { duration: '40s', target: 100 }, // 2. Hold the load for 40 seconds.
        { duration: '10s', target: 0 },   // 3. Ramp down.
    ],
    thresholds: {
        // The test fails if more than 0.1% of requests fail.
        'http_req_failed': ['rate<0.001'],
        // The test fails if the 95th percentile of request duration is over 800ms.
        'http_req_duration': ['p(95)<800'],
    },
};

// --- Main k6 Virtual User Function ---
export default function () {
    if (!targetUrl) {
        failedRequests.add(1);
        console.error("FATAL: TARGET_URL environment variable was not provided to the k6 script.");
        return;
    }

    const payload = JSON.stringify({
        // The server DTO "EchoMessage.java" expects the key "message".
        message: 'Hello from k6!',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(targetUrl, payload, params);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response body contains echoed message': (r) => {
            try {
                // Ensure the echoed message in the JSON response matches what was sent.
                return r.json('message') === 'Hello from k6!';
            } catch (e) {
                return false; // json() will throw an error if the body is not valid JSON
            }
        },
    });

    if (!success) {
        failedRequests.add(1);
    }
}