# Quarkus & GraalVM Communication Patterns Lab (v3.0-websockets)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Tag](https://img.shields.io/github/v/tag/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/tags)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This README documents the state of the project at the **v3.0-websockets** tag. This phase introduces a full-duplex,
bidirectional communication pattern using **WebSockets**. The implementation includes a real-time chat server and a
dual-client simulation UI for an intuitive demonstration.

Additionally, this phase marks a significant refactoring of the benchmark suite. All load tests (REST, SSE, WebSockets)
have been unified under the **`k6`** framework for consistent, powerful, and maintainable performance analysis.

## Technology Stack

- **Backend:** Java 21 & Quarkus 3.x
- **Runtimes:** JVM, GraalVM Native
- **Containerization:** Docker & Docker Compose
- **Demo Client:** Static HTML/CSS/JS served via Nginx
- **Benchmark Tooling:** [`k6`](https://github.com/grafana/k6) (with a build including a [custom plugin](https://github.com/phymbert/xk6-sse) for SSE)

---

## How to Run This Phase

### Prerequisites

- Docker and Docker Compose
- A shell environment (Bash, Zsh) with `curl`.

### 1. Clone and Check Out the Tag

Ensure you are on the correct version of the code.

```bash
git clone https://github.com/apenlor/quarkus-communication-patterns-lab.git
cd quarkus-communication-patterns-lab
git checkout v3.0-websockets
```

### 2. Build and Run All Services

This command builds the JVM and Native images and starts all services.

```bash
docker-compose up -d --build
```

*Note: The first native compilation may take several minutes.*

Services will be available at:

- **JVM Server:** `http://localhost:8080`
- **Native Server:** `http://localhost:8081`
- **Demo Client:** `http://localhost:8082`

### 3. Explore the Demos

Open your browser to the **Demo Client at [`http://localhost:8082`](http://localhost:8082)**.

- The **REST Demo** tests the synchronous echo endpoint.
- The **SSE Demo** shows a live stream of data from both services.
- The **WebSocket Demo** provides a dual-client chat simulation, allowing you to send messages as "User A" and see them
  appear in "User B's" window in real-time.

---

## Benchmarking for Phase 3.0

The entire benchmark suite now uses a unified `k6` runner system. All scripts are run from the repository root and
accept the target container name (`server-jvm` or `server-native`) as an argument.

### 1. Measure Startup Time

This script measures the "time to readiness" by parsing the application's startup logs.

```bash
# Measure the JVM service
./scripts/measure-startup.sh server-jvm

# Measure the Native service
./scripts/measure-startup.sh server-native
```

### 2. Run the REST Load Test

This script uses `k6` to measure the throughput and latency of the `POST /echo` endpoint.

```bash
# Benchmark the JVM service
./bench-clients/rest-benchmark.sh server-jvm

# Benchmark the Native service
./bench-clients/rest-benchmark.sh server-native
```

### 3. Run the SSE Load Test

This script uses a custom `k6` build to test concurrent, persistent SSE connections.

```bash
# Benchmark the JVM service
./bench-clients/sse-benchmark.sh server-jvm

# Benchmark the Native service
./bench-clients/sse-benchmark.sh server-native
```

### 4. Run the WebSocket Load Test

This script uses `k6`'s native WebSocket support to test the server's ability to handle concurrent, bidirectional
connections, measuring connection rates and message round-trip time.

```bash
# Benchmark the JVM service
./bench-clients/ws-benchmark.sh server-jvm

# Benchmark the Native service
./bench-clients/ws-benchmark.sh server-native
```

<details>
<summary>Click to see sample WebSocket benchmark output</summary>

```bash
  █ THRESHOLDS 

    failed_connections
    ✓ 'count==0' count=0

    time_to_first_message
    ✓ 'p(95)<1500' p(95)=410.4ms

    websocket_message_rtt
    ✓ 'p(95)<500' p(95)=6ms


  █ TOTAL RESULTS 

    checks_total.......: 167     1.929165/s
    checks_succeeded...: 100.00% 167 out of 167
    checks_failed......: 0.00%   0 out of 167

    ✓ WebSocket handshake successful

    CUSTOM
    failed_connections......: 0      0/s
    time_to_first_message...: avg=124.22ms min=0s med=106ms max=415ms p(90)=114.4ms p(95)=410.4ms
    websocket_message_rtt...: avg=3.15ms min=0s med=3ms max=16ms p(90)=5ms p(95)=6ms    

    WEBSOCKET
    ws_connecting...........: avg=4.29ms min=680.43µs med=4.06ms max=11.74ms p(90)=6.17ms p(95)=7.54ms 
    ws_msgs_received........: 35705  412.460166/s
    ws_msgs_sent............: 818    9.449445/s
    ws_sessions.............: 167    1.929165/s
```

</details>

### 5. Stop the Environment

```bash
docker-compose down -v
```

---

## Project Roadmap (State at v3.0-websockets)

- ✅ **[Phase 1.0: The REST Baseline](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v1.0-rest)**
- ✅ **[Phase 2.0: Server-Sent Events](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v2.0-sse)**
- ✅ **Phase 3.0: WebSockets** - *(This is the current phase)*
- ⏳ **Phase 4.0: gRPC**
- 📋 **Phase 5.0: RSocket**
- 📋 **Phase 6.0: Final Analysis**