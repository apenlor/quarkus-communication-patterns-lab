document.addEventListener('DOMContentLoaded', () => {

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = wsProtocol + '//' + window.location.hostname + ':8080/ws/chat';

    /**
     * A class to manage a single WebSocket chat client instance,
     * including its connection and associated DOM elements.
     */
    class ChatClient {
        /**
         * @param {string} id The client identifier ('a' or 'b').
         */
        constructor(id) {
            this.id = id;
            this.socket = null;

            // Cache all DOM elements for this instance using standard string concatenation
            this.elements = {};
            this.elements.messageInput = document.getElementById('message-input-' + id);
            this.elements.sendButton = document.getElementById('send-button-' + id);
            this.elements.chatForm = document.getElementById('chat-form-' + id);
            this.elements.messagesContainer = document.getElementById('messages-' + id);
            this.elements.statusIndicator = document.getElementById('status-indicator-' + id);
            this.elements.statusText = document.getElementById('status-text-' + id);

            // Bind event listeners
            this.elements.chatForm.addEventListener('submit', this.sendMessage.bind(this));
        }

        connect() {
            // Defensive check to avoid null elements if HTML is malformed
            if (!this.elements.messageInput) {
                console.error('Initialization failed for client ' + this.id + '. Check HTML element IDs.');
                return;
            }

            this.socket = new WebSocket(wsUrl);

            this.socket.onopen = () => {
                console.log('Client ' + this.id + ': WebSocket connection established');
                this.updateStatus(true, 'Connected');
                this.addSystemMessage('Connection successful.');
                this.elements.messageInput.disabled = false;
                this.elements.sendButton.disabled = false;
            };

            this.socket.onmessage = (event) => {
                const messageContent = event.data;
                console.log('Client ' + this.id + ': Message received:', messageContent);
                // Server filters, so any message received is from another user.
                this.addChatMessage(messageContent, 'received');
            };

            this.socket.onclose = () => {
                console.log('Client ' + this.id + ': WebSocket connection closed');
                this.updateStatus(false, 'Disconnected');
                this.addSystemMessage('Connection lost. Reconnecting in 5s...');
                this.elements.messageInput.disabled = true;
                this.elements.sendButton.disabled = true;
                setTimeout(() => this.connect(), 5000);
            };

            this.socket.onerror = (error) => {
                console.error('Client ' + this.id + ': WebSocket error:', error);
                this.addSystemMessage('A connection error occurred.');
                this.updateStatus(false, 'Error');
            };
        }

        updateStatus(isConnected, text) {
            this.elements.statusIndicator.className = 'status-indicator ' + (isConnected ? 'connected' : 'disconnected');
            this.elements.statusText.textContent = text;
        }

        addSystemMessage(text) {
            const messageElement = document.createElement('div');
            messageElement.className = 'message system';
            messageElement.textContent = text;
            this.elements.messagesContainer.appendChild(messageElement);
            this.scrollToBottom();
        }

        addChatMessage(content, type) {
            const messageElement = document.createElement('div');
            messageElement.className = 'message ' + type;
            messageElement.textContent = content;
            this.elements.messagesContainer.appendChild(messageElement);
            this.scrollToBottom();
        }

        sendMessage(event) {
            event.preventDefault();
            const messageContent = this.elements.messageInput.value.trim();
            if (messageContent && this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send(messageContent);
                // Optimistic UI update for the sender
                this.addChatMessage(messageContent, 'sent');
                this.elements.messageInput.value = '';
            }
        }

        scrollToBottom() {
            const container = this.elements.messagesContainer;
            container.scrollTop = container.scrollHeight;
        }
    }

    // --- Main Execution ---
    // Create and connect two independent chat clients.
    const clientA = new ChatClient('a');
    const clientB = new ChatClient('b');

    clientA.connect();
    clientB.connect();
});