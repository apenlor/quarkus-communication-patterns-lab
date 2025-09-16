document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('echo-form');
    const messageInput = document.getElementById('message');
    const responseElement = document.getElementById('response');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        // The URL for our Quarkus API, exposed via Docker Compose
        const apiUrl = 'http://localhost:8080/echo';
        const message = messageInput.value;

        responseElement.textContent = 'Sending request...';

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
            responseElement.textContent = JSON.stringify(data, null, 2);

        } catch (error) {
            console.error('Error sending request:', error);
            responseElement.textContent = `An error occurred: ${error.message}`;
        }
    });
});