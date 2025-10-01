# Quarkus & GraalVM Communication Patterns Lab

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/releases/latest)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A repository that systematically demonstrates, benchmarks, and compares common client-server communication patterns on
the Quarkus framework. It contrasts the performance characteristics of the standard JVM against a GraalVM native
executable.

## Core principles

- **Reproducibility:** The entire lab is runnable via Docker Compose, with zero local dependencies besides Docker
  itself.
- **Phased Development:** The project is built additively, with each phase frozen by an immutable Git tag.
- **Documented Evolution:** Architectural decisions are captured in **Architectural Decision Records (ADRs)** and
  detailed within Pull Requests.

---

## Quickstart (latest version)

This guide runs the latest version of the project from the `main` branch.

**Prerequisites:**

- Docker and Docker Compose
- A shell environment (Bash, Zsh)

### 1. Build & run all services

This single command builds all necessary images (application and tooling) and starts the services in the background.

```bash
docker compose up -d --build
```

*Note: The first time you run this, the native compilation may take several minutes.*

Services are available at:

- **JVM Server:** HTTP at `http://localhost:8080`, gRPC at `localhost:9001`
- **Native Server:** HTTP at `http://localhost:8081`, gRPC at `localhost:9002`
- **Demo Client Hub:** `http://localhost:8082`

### 2. Explore the demos

Open your browser to the **Demo Client Hub at `http://localhost:8082`**. This page is a navigation hub linking to demos
for all implemented communication patterns.

### 3. Stop the environment

```bash
docker compose down -v
```

---

## Benchmark suite

A primary goal of this repository is to provide a clear, quantitative comparison of different communication patterns and
runtimes.

### 1. Startup time measurement

A custom script ([`bench-clients/startup-benchmark.sh`](bench-clients/startup-benchmark.sh)) repeatedly creates a fresh container and
parses application logs to determine the precise "time-to-readiness," clearly measuring the advantage of GraalVM native
compilation.

**How to Run:**

```bash
# Measure the JVM service
./scripts/measure-startup.sh server-jvm

# Measure the Native service
./scripts/measure-startup.sh server-native
```

### 2. Load and performance testing

The project uses a hybrid approach to load testing, selecting the best tool for each protocol's unique characteristics.
All tests are executed via simple wrapper scripts in the [`bench-clients/`](bench-clients) directory.

**k6-based Benchmarks (REST, SSE, WebSockets)**

For HTTP-based protocols, the project has standardized on **[`k6`](https://k6.io/)** for its powerful scripting and
consistent reporting.

```bash
# REST Benchmark
./bench-clients/rest-benchmark.sh server-jvm
./bench-clients/rest-benchmark.sh server-native

# SSE Benchmark
./bench-clients/sse-benchmark.sh server-jvm
./bench-clients/sse-benchmark.sh server-native

# WebSocket Benchmark
./bench-clients/ws-benchmark.sh server-jvm
./bench-clients/ws-benchmark.sh server-native
```

**Custom Java Benchmark (gRPC)**

For our stateful, bidirectional gRPC stream, off-the-shelf tools proved insufficient. We engineered a **custom,
multithreaded Java client** to provide a high-fidelity benchmark that accurately measures end-to-end broadcast latency
under a realistic, rate-limited load.

```bash
# gRPC Benchmark
./bench-clients/grpc-benchmark.sh server-jvm
./bench-clients/grpc-benchmark.sh server-native
```

For detailed instructions and sample outputs for any specific phase, please refer to the `README.md` at its
corresponding Git Tag.

---

## Project evolution: A phase-by-phase journey

This project was built incrementally to demonstrate a clean, evolutionary design process. Each phase represents a
significant new capability. To follow the engineering narrative, start by reviewing the Pull Request, which contains
the "why" behind the decisions. To see the final, clean implementation of a phase, browse the code at the corresponding
Git Tag.

| Phase                             | Status | Focus                                                                                                     | Key Artifacts                                                                                                                                                                                      |
|-----------------------------------|:------:|-----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1.0: REST Baseline**            |   ✅    | Establish the project structure, a JSON REST API, and baseline JVM performance metrics.                   | [Code @ `v1.0-rest`](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v1.0-rest) <br/> [PR #1](https://github.com/apenlor/quarkus-communication-patterns-lab/pull/1)             |
| **2.0: Server-Sent Events (SSE)** |   ✅    | Introduce one-way server-to-client streaming and add the GraalVM native executable for comparison.        | [Code @ `v2.0-sse`](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v2.0-sse) <br/> [PR #4](https://github.com/apenlor/quarkus-communication-patterns-lab/pull/4)               |
| **3.0: WebSockets**               |   ✅    | Implement full-duplex communication and unify the entire benchmark suite on a modern `k6` toolchain.      | [Code @ `v3.0-websockets`](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v3.0-websockets) <br/> [PR #6](https://github.com/apenlor/quarkus-communication-patterns-lab/pull/6) |
| **4.0: gRPC**                     |   ✅    | Implement a contract-first RPC service and engineer a custom benchmark for stateful streaming.            | [Code @ `v4.0-grpc`](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v4.0-grpc) <br/> [PR #8](https://github.com/apenlor/quarkus-communication-patterns-lab/pull/8)             |
| **5.0: RSocket (Investigation)**  |   ❌    | Investigate RSocket and formally exclude it due to significant incompatibilities and technical obstacles. | [ADR-003](docs/adr/003-exclude-rsocket-implementation.md)                                                                                                                                          |
| **6.0: Final Analysis**           |   ⏳    | Consolidate all benchmark results and write a final analysis to complete the project's narrative.         | *(In Progress)*                                                                                                                                                                                    |