# Quarkus & GraalVM Communication Patterns Lab (v4.0-grpc)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Tag](https://img.shields.io/github/v/tag/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/tags)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This README documents the state of the project at the **v4.0-grpc** tag. This phase introduces a high-performance,
contract-first communication pattern using **gRPC**. The implementation features a bidirectional streaming chat service
defined with Protocol Buffers (`.proto`).

A key deliverable for this phase is a standalone Java command-line client, which serves as the primary demonstration
tool. The benchmark for this phase is also a custom-built, multithreaded Java application, needed for
measuring the performance of a stateful, bidirectional stream.

## Technology stack

- **Backend:** Java 21 & Quarkus 3.x
- **Runtimes:** JVM, GraalVM Native
- **RPC Framework:** gRPC with Protocol Buffers
- **Containerization:** Docker & Docker Compose
- **Demo Client:** Static HTML/JS (for instructions) & a standalone Java CLI Application
- **Benchmark Tooling:**
    - `k6` for HTTP-based protocols
    - **Custom Java Benchmark Client** (using `HdrHistogram`) for gRPC

---

## How to run this phase

### Prerequisites

- Docker and Docker Compose
- A Java 21+ JDK and Maven (`mvn`) for running the demo client.
- A shell environment (Bash, Zsh).

### 1. Clone and check out the tag

Ensure you are on the correct version of the code.

```bash
git clone https://github.com/apenlor/quarkus-communication-patterns-lab.git
cd quarkus-communication-patterns-lab
git checkout v4.0-grpc
```

### 2. Build and run all services

This command builds the JVM and Native images and starts all services.

```bash
docker-compose up -d --build
```

Services will be available at:

- **JVM Server:** HTTP at `http://localhost:8080`, gRPC at `localhost:9001`
- **Native Server:** HTTP at `http://localhost:8081`, gRPC at `localhost:9002`
- **Web Demo Hub:** `http://localhost:8082`

### 3. Explore the demos

Open your browser to the **Web Demo Hub at [`http://localhost:8082`](http://localhost:8082)**. From there, you can
navigate to the demos for each protocol.

The gRPC demo page provides instructions for running the interactive command-line client, which is the primary way to
test the gRPC service. You will need at least two terminals to simulate a chat.

**First, build the CLI client:**

```bash
# From the project root
(cd demo-client/grpc-cli && ./mvnw clean package)
```

**Next, run two instances (e.g., in two separate terminals) connecting to the same server port.**

```bash
# Terminal 1 (connecting to the JVM server as "Alice")
java -jar demo-client/grpc-cli/target/grpc-cli-client-*.jar localhost 9001

# Terminal 2 (connecting to the JVM server as "Bob")
java -jar demo-client/grpc-cli/target/grpc-cli-client-*.jar localhost 9001
```

To test the native server, use port `9002`.

---

## Benchmarking for phase 4.0

All benchmark scripts are run from the repository root and accept the target container name (`server-jvm` or
`server-native`) as an argument.

### 1. Measure startup time

This script measures the "time to readiness" by parsing the application's startup logs.

```bash
# Measure the JVM service
./scripts/measure-startup.sh server-jvm

# Measure the Native service
./scripts/measure-startup.sh server-native
```

### 2. Run all load tests

This section provides the commands to run the load tests for all implemented protocols.

<details>
<summary><strong>REST Benchmark (k6)</strong></summary>

This test measures the throughput and latency of the synchronous `POST /echo` endpoint.

```bash
./bench-clients/rest-benchmark.sh server-jvm
./bench-clients/rest-benchmark.sh server-native
```

</details>

<details>
<summary><strong>SSE Benchmark (k6 with xk6-sse)</strong></summary>

This test uses a custom `k6` build to measure concurrent, persistent SSE connections.

```bash
./bench-clients/sse-benchmark.sh server-jvm
./bench-clients/sse-benchmark.sh server-native
```

</details>

<details>
<summary><strong>WebSocket Benchmark (k6)</strong></summary>

This test measures the server's ability to handle concurrent, bidirectional WebSocket connections and the end-to-end
broadcast latency.

```bash
./bench-clients/ws-benchmark.sh server-jvm
./bench-clients/ws-benchmark.sh server-native
```

</details>

#### gRPC Benchmark (Custom Java Client)

This test uses our custom-built, multi-threaded Java client to measure the end-to-end broadcast latency of the stateful
`BidiChat` service.

```bash
./bench-clients/grpc-benchmark.sh server-jvm
./bench-clients/grpc-benchmark.sh server-native
```

An example of the detailed output:

```bash
-------------------- Benchmark Results --------------------
Total Messages Measured: 1274313
Total Timeouts: 0 (indicates back-pressure)
Throughput: 42477,10 msg/sec
---------------------------------------------------------
Latency (microseconds):
  min:      0
  mean:     2,00
  p50 (median): 1
  p90:      5
  p99:      13
  p99.9:    18
  max:      83230
---------------------------------------------------------
```

### 3. Stop the environment

When you are finished, stop and remove all containers.

```bash
docker-compose down -v
```

---

## Project roadmap (state at v4.0-grpc)

- ✅ **[Phase 1.0: The REST Baseline](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v1.0-rest)**
- ✅ **[Phase 2.0: Server-Sent Events](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v2.0-sse)**
- ✅ **[Phase 3.0: WebSockets](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v3.0-websockets)**
- ✅ **Phase 4.0: gRPC** - *(This is the current phase)*
- ⏳ **Phase 5.0: RSocket**
- 📋 **Phase 6.0: Final Analysis**