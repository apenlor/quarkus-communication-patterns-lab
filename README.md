# Quarkus & GraalVM Communication Patterns Lab

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Tag](https://img.shields.io/github/v/tag/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/tags)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A repository that systematically demonstrates, benchmarks, and compares common client-server communication patterns on
the Quarkus framework. The core objective is to contrast the performance characteristics (startup time, memory usage,
throughput, latency) of the standard JVM runtime versus a GraalVM native executable.

## Core Principles

This project is engineered with the following principles to ensure it serves as a high-quality, reproducible example of
modern software engineering:

* **Reproducibility:** The entire lab-server, demo client, and benchmark tools—is orchestrated via Docker Compose and
  runnable with a single command.
* **Phased Development:** The project is built additively. Each completed phase is frozen with an immutable, annotated
  Git tag, creating a logical and reviewable history.
* **Isolation:** Each communication pattern is implemented as a distinct, cleanly separated endpoint within a single
  microservice.

## Technology Stack

* **Backend:** Java 21 (LTS) & Quarkus 3.x
* **Build System:** Maven 3.9+
* **Containerization:** Docker & Docker Compose
* **Demo Client:** Static HTML/CSS/JS served via Nginx
* **Benchmark Tooling:** `wrk` (via Docker), Bash Scripts
* **CI/CD:** GitHub Actions

## Repository Structure

The repository is organized to clearly separate concerns, making it easy to navigate and understand.

| File / Directory     | Description                                                                                                                              |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `.github/workflows/` | Contains the CI/CD pipelines (e.g., [`ci.yml`](.github/workflows/ci.yml)) powered by GitHub Actions.                                     |
| `bench-clients/`     | Holds client-side scripts (`wrk`, `k6`, etc.) used to run performance and load testing benchmarks.                                       |
| `demo-client/`       | A static HTML/JS/CSS frontend served via a dedicated Nginx container to demonstrate API functionality.                                   |
| `scripts/`           | Contains operational helper scripts, such as the [`measure-startup.sh`](scripts/measure-startup.sh) tool for benchmarking startup times. |
| `server/`            | The core Quarkus Maven project containing the Java backend service.                                                                      |
| `docker-compose.yml` | The master orchestration file. Running `docker-compose up` builds and starts the entire lab.                                             |
| `README.md`          | Project documentation, including the mission, quickstart guide, and roadmap.                                                             |
| `LICENSE`            | The MIT License under which this project is distributed.                                                                                 |


## Quickstart

This repository is designed to be run with a single command. At this phase, it will launch the JVM-based server and a
static Nginx client.

**Prerequisites:**

* Docker
* Docker Compose

**Instructions:**

1. Clone the repository:
   ```bash
   git clone git@github.com:apenlor/quarkus-communication-patterns-lab.git
   cd quarkus-communication-patterns-lab
   ```
2. Build and run all services:
   ```bash
   docker-compose up -d --build
   ```
3. Access the services:
    * **Demo Client UI:** [http://localhost:8082](http://localhost:8082)
    * **JVM API Health:** [http://localhost:8080/q/health/live](http://localhost:8080/q/health/live)

---

## Current Phase: 1.0 - REST Baseline

This phase establishes the foundational structure of the project. It implements a standard synchronous, request-response
API using JSON over HTTP to serve as the performance baseline against which all subsequent patterns will be measured.

### Running the Benchmarks for Phase 1.0

The benchmark scripts are designed to be run from the repository root while the services are running.

#### 1. Measuring Startup Time

This script measures the "time to readiness" by cleanly starting the container and polling its health endpoint. It
provides a precise metric for the cold start performance of the JVM-based application.

**Command:**

```bash
  ./scripts/measure_startup.sh server-jvm
```

**Sample Output:**

```
--- Measuring startup time for service: server-jvm ---
...
✅ Service is ready!
Startup Time: 1.709 seconds
--- Cleaning up containers ---
```

#### 2. Running the Load Test

This script uses `wrk` to measure the throughput and latency of the `POST /echo` endpoint. It simulates a high number of
concurrent users to test the server's runtime performance under load.

**Command:**

```bash
  ./bench-clients/rest_benchmark.sh
```

**Sample Output:**

```
--- Starting REST API Benchmark ---
Running 30s test @ http://localhost:8080/echo
  4 threads and 50 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.02ms    5.60ms 155.62ms   98.60%
    Req/Sec     7.22k     2.15k   14.70k    75.74%
  877950 requests in 30.09s, 144.01MB read
Requests/sec:  29177.88
Transfer/sec:      4.79MB
--- REST API Benchmark Complete ---
```

## Project Roadmap

This repository is developed in distinct, tagged phases. The `main` branch will always contain the latest completed
phase.

* ✅ **Phase 1.0: REST Baseline** - Synchronous request-response. [`(Current: v1.0-rest)`](https://github.com/apenlor/quarkus-communication-patterns-lab/releases/tag/v1.0-rest)
* ◻️ **Phase 2.0: Server-Sent Events (SSE)** - Unidirectional server-to-client streaming.
* ◻️ **Phase 3.0: WebSockets** - Bidirectional, full-duplex communication.
* ◻️ **Phase 4.0: gRPC** - High-performance, contract-first RPC with bidirectional streaming.
* ◻️ **Phase 5.0: RSocket** - Modern, reactive protocol with multiple interaction models.
* ◻️ **Phase 6.0: Final Analysis** - Complete benchmark results and comparative analysis.

*(Links to tags/releases will be added as they are created).*