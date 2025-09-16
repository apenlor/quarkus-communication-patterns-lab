document.addEventListener('DOMContentLoaded', () => {
    // --- Connect to JVM Service ---
    const jvmStatus = document.getElementById('status-jvm');
    const jvmTicker = document.getElementById('events-jvm');
    const jvmSource = new EventSource('http://localhost:8080/stream/ticker');

    jvmSource.onopen = () => {
        jvmStatus.textContent = 'Connected';
    };

    jvmSource.onmessage = (event) => {
        const tickerData = JSON.parse(event.data);
        jvmTicker.textContent = JSON.stringify(tickerData, null, 2);
    };

    jvmSource.onerror = () => {
        jvmStatus.textContent = 'Disconnected';
    };

    // --- Connect to Native Service ---
    const nativeStatus = document.getElementById('status-native');
    const nativeTicker = document.getElementById('events-native');
    const nativeSource = new EventSource('http://localhost:8081/stream/ticker');

    nativeSource.onopen = () => {
        nativeStatus.textContent = 'Connected';
    };

    nativeSource.onmessage = (event) => {
        const tickerData = JSON.parse(event.data);
        nativeTicker.textContent = JSON.stringify(tickerData, null, 2);
    };

    nativeSource.onerror = () => {
        nativeStatus.textContent = 'Disconnected';
    };
});