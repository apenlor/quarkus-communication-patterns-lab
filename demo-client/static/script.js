document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('echo-form');
    const messageInput = document.getElementById('message');
    const statusDiv = document.getElementById('status');
    const responseOutput = document.getElementById('response-output');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        // The URL for our Quarkus API, exposed via Docker Compose
        const apiUrl = 'http://localhost:8080/echo';
        const message = messageInput.value;

        statusDiv.textContent = 'Sending request...';
        responseOutput.textContent = '';

        try {
            const requestPayload = {
                message: message
            };

            const response = await fetch(apiUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestPayload),
            });

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            const data = await response.json();

            // Pretty-print the JSON response
            responseOutput.textContent = JSON.stringify(data, null, 2);
            statusDiv.textContent = `Success! Received response with status ${response.status}.`;

        } catch (error) {
            console.error('Error sending request:', error);
            responseOutput.textContent = `An error occurred: ${error.message}`;
            statusDiv.textContent = 'Request failed.';
        }
    });
});