// File: demo-client/static/sse/script.js
document.addEventListener('DOMContentLoaded', () => {
    // --- Connect to JVM Service ---
    const jvmStatus = document.getElementById('jvm-status');
    const jvmTicker = document.getElementById('jvm-ticker');
    const jvmSource = new EventSource('http://localhost:8080/stream/ticker');

    jvmSource.onopen = () => {
        jvmStatus.textContent = 'Connected';
        jvmStatus.className = 'status connected';
    };

    jvmSource.onmessage = (event) => {
        const tickerData = JSON.parse(event.data);
        jvmTicker.textContent = JSON.stringify(tickerData, null, 2);
    };

    jvmSource.onerror = () => {
        jvmStatus.textContent = 'Disconnected';
        jvmStatus.className = 'status disconnected';
    };

    // --- Connect to Native Service ---
    const nativeStatus = document.getElementById('native-status');
    const nativeTicker = document.getElementById('native-ticker');
    const nativeSource = new EventSource('http://localhost:8081/stream/ticker');

    nativeSource.onopen = () => {
        nativeStatus.textContent = 'Connected';
        nativeStatus.className = 'status connected';
    };

    nativeSource.onmessage = (event) => {
        const tickerData = JSON.parse(event.data);
        nativeTicker.textContent = JSON.stringify(tickerData, null, 2);
    };

    nativeSource.onerror = () => {
        nativeStatus.textContent = 'Disconnected';
        nativeStatus.className = 'status disconnected';
    };
});