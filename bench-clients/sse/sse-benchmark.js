import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import sse from 'k6/x/sse';

const timeToFirstMessage = new Trend('time_to_first_message', true);
const failedConnections = new Counter('failed_connections');
const messagesReceived = new Counter('messages_received');

const targetUrl = __ENV.TARGET_URL;

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
    await new Promise((resolve) => {
        let connectionStartTime;

        const response = sse.open(targetUrl, {}, function (client) {
            let firstMessage = true;

            client.on('open', function () {
                connectionStartTime = new Date().getTime();
            });

            client.on('event', function (event) {
                messagesReceived.add(1);
                const data = event.data;
                check(data, { 'message data is not empty': (d) => d && d.length > 0 });

                if (firstMessage && connectionStartTime) {
                    const now = new Date().getTime();
                    timeToFirstMessage.add(now - connectionStartTime);
                    firstMessage = false;
                }
            });

            client.on('error', function (e) {
                failedConnections.add(1);
                resolve();
            });

            setTimeout(() => {
                client.close();
                resolve();
            }, 20000); // Stay connected for 20 seconds
        });

        if (!check(response, { 'handshake_status_is_200': (r) => r && r.status === 200 })) {
            failedConnections.add(1);
            resolve();
        }
    });
}