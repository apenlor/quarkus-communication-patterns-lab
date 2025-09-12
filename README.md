# Quarkus & GraalVM Communication Patterns Lab (v2.0-sse)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Tag](https://img.shields.io/github/v/tag/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/tags)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This README documents the state of the project at the **v2.0-sse** tag. At this phase, the repository demonstrates both
a synchronous REST API and a server-to-client streaming API using Server-Sent Events (SSE). It also introduces the
GraalVM native executable for direct performance comparison against the JVM.

## Technology Stack

- **Backend:** Java 21 & Quarkus 3.x
- **Runtimes:** JVM, GraalVM Native
- **Containerization:** Docker & Docker Compose
- **Demo Client:** Static HTML/CSS/JS served via Nginx
- **Benchmark Tooling:** [`wrk`](https://github.com/wg/wrk), [`k6`](https://github.com/grafana/k6) (with custom [
  `xk6-sse`](https://github.com/phymbert/xk6-sse) extension)

---

## How to Run This Phase

### Prerequisites

- Docker and Docker Compose
- A shell environment (Bash, Zsh) with `curl` and `bc`

### 1. Clone and Check Out the Tag

Ensure you are on the correct version of the code.

```bash
	git clone https://github.com/apenlor/quarkus-communication-patterns-lab.git
	cd quarkus-communication-patterns-lab
	git checkout v2.0-sse
```

### 2. Build and Run All Services

This single command will build both the JVM and Native images (if they don't already exist) and then start all the
services in the background.

```bash
	docker compose up -d --build
```

*Note: The first time you run this command, the native compilation may take minutes.*

Services will be available at:

- **JVM Server:** [`http://localhost:8080`](http://localhost:8080)
- **Native Server:** [`http://localhost:8081`](http://localhost:8081)
- **Demo Client:** [`http://localhost:8082`](http://localhost:8082)

### 3. Explore the Demos

Open your browser to the **Demo Client at [`http://localhost:8082`](http://localhost:8082)**.

- The **REST Demo** allows you to send a JSON message and receive an echo.
- The **SSE Demo** connects to both the JVM and Native services to display a live stream of ticker data.

---

## Benchmarking for Phase 2.0

All benchmark scripts are designed to be run from the repository root while the services are running. They accept a
logical service name (`server-jvm` or `server-native`) as an argument.

### 1. Measure Startup Time

This script accurately measures the "time to readiness" for a service by polling its health check endpoint.

```bash
# Measure the JVM service
./scripts/measure-startup.sh server-jvm

# Measure the Native service
./scripts/measure-startup.sh server-native
```

**Observation:** The native executable demonstrates a startup time that is an order of magnitude faster than its JVM
counterpart.

### 2. Run the REST Load Test

This script uses [`wrk`](https://github.com/wg/wrk) to measure the throughput and latency of the synchronous
`POST /echo` endpoint.

```bash
# Benchmark the JVM service's REST endpoint
./bench-clients/rest_benchmark.sh server-jvm

# Benchmark the Native service's REST endpoint
./bench-clients/rest_benchmark.sh server-native
```

### 3. Run the SSE Load Test

This script uses a custom-built [`k6`](https://github.com/grafana/k6) binary with the [
`xk6-sse`](https://github.com/phymbert/xk6-sse) extension to test the server's ability to handle
concurrent, persistent SSE connections.

```bash
# Benchmark the JVM service's SSE endpoint
./bench-clients/sse_benchmark.sh server-jvm

# Benchmark the Native service's SSE endpoint
./bench-clients/sse_benchmark.sh server-native
```

### 4. Stop the Environment

```bash
docker compose down -v
```

---

## Project Roadmap (State at v2.0-sse)

- ✅ **[Phase 1.0: The REST Baseline](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v1.0-rest)**
- ✅ **Phase 2.0: Server-Sent Events (SSE)** - *(This is the current phase)*
- ⏳ **Phase 3.0: WebSockets**
- 📋 **Phase 4.0: gRPC**
- 📋 **Phase 5.0: RSocket**
- 📋 **Phase 6.0: Final Analysis**