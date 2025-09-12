import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import sse from 'k6/x/sse';

const timeToFirstMessage = new Trend('time_to_first_message', true);
const failedConnections = new Counter('failed_connections');
const messagesReceived = new Counter('messages_received');

const targetUrl = typeof __ENV !== 'undefined' ? __ENV.TARGET_URL : null;

export const options = {
    stages: [
        { duration: '20s', target: 50 },
        { duration: '40s', target: 50 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'failed_connections': ['count==0'],
        'time_to_first_message': ['p(95)<1500'],
    },
};

export default async function () {
    if (!targetUrl) {
        failedConnections.add(1);
        console.error("FATAL: TARGET_URL environment variable was not provided to the k6 script.");
        return;
    }

    await new Promise((resolve) => {
        let client;
        try {
            let connectionStartTime;

            const response = sse.open(targetUrl, {}, function (c) {
                client = c;
                let firstMessage = true;

                client.on('open', function () {
                    connectionStartTime = new Date().getTime();
                });

                client.on('event', function (event) {
                    messagesReceived.add(1);
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

            if (!check(response, { 'handshake_status_is_200': (r) => r && r.status === 200 })) {
                failedConnections.add(1);
                return resolve();
            }

            setTimeout(() => {
                if (client) {
                    client.close();
                }
                resolve();
            }, 20000);

        } catch (e) {
            console.error(`An unexpected error occurred during setup: ${e}`);
            failedConnections.add(1);
            if (client) {
                client.close();
            }
            resolve();
        }
    });
}