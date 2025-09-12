# Quarkus & GraalVM Communication Patterns Lab

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Tag](https://img.shields.io/github/v/tag/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/tags)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A repository that systematically demonstrates, benchmarks, and compares common client-server communication patterns on
the Quarkus framework. It contrasts the performance characteristics of the standard JVM against a GraalVM native
executable.

## Core Principles

- **Reproducibility:** The entire lab is runnable via Docker Compose, with zero local dependencies besides Docker
  itself.
- **Phased Development:** The project is built additively, with each phase frozen by an immutable Git tag.
- **Documented Evolution:** Architectural decisions are captured in detail within Pull Requests, creating a rich,
  reviewable history of the project's engineering journey.

---

## Quickstart (Latest Version)

This guide runs the latest version of the project from the `main` branch.

**Prerequisites:**

- Docker and Docker Compose
- A shell environment (Bash, Zsh) with `bc`

### 1. Build & Run all services

This single command builds all necessary images (application and tooling) and starts the services in the background.

```bash
docker compose up -d --build
```

*Note: The first time you run this, the native compilation and custom benchmark image builds may take several minutes.*

Services are available at:

- **JVM Server:** `http://localhost:8080`
- **Native Server:** `http://localhost:8081`
- **Demo Client:** `http://localhost:8082`

### 3. Explore the Demos

Open your browser to the **Demo Client at `http://localhost:8082`**. This page is a navigation hub linking to demos for
all implemented communication patterns.

### 4. Stop the Environment

```bash
docker compose down -v
```

---

## Benchmarking Suite & Final Analysis

A primary goal of this repository is to provide a clear, quantitative comparison of the different communication
patterns.

Each completed phase includes its own dedicated, runnable benchmark scripts. The final phase of this project (**Phase
6.0**) will culminate in the creation of a unified benchmarking framework. This will include master scripts to
orchestrate all load tests for all protocols against both JVM and native runtimes, and will generate a final summary
report with clear visualizations.

**In the meantime, to run the specific benchmarks for a completed phase (e.g., REST or SSE), please refer to the
detailed instructions in the `README.md` of its corresponding Git Tag.**

---

## Project Evolution: A Phase-by-Phase Journey

This project was built incrementally to demonstrate a clean, evolutionary design process. Each phase represents a
significant new capability. To follow the engineering narrative, start by reviewing the Pull Request, which contains
the "why" behind the decisions. To see the final, clean implementation of a phase, browse the code at the corresponding
Git Tag.

| Phase                             | Status | Focus                                                                                               | Key Artifacts                                                                                                                                                                          |
|-----------------------------------|:------:|-----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1.0: REST Baseline**            |   ✅    | Establish the project structure, a JSON REST API, and baseline JVM performance metrics.             | [Code @ `v1.0-rest`](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v1.0-rest) <br/> [PR #1](https://github.com/apenlor/quarkus-communication-patterns-lab/pull/1) |
| **2.0: Server-Sent Events (SSE)** |   ✅    | Introduce one-way server-to-client streaming and add the GraalVM native executable for comparison.  | [Code @ `v2.0-sse`](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v2.0-sse) <br/> [PR #4](https://github.com/apenlor/quarkus-communication-patterns-lab/pull/4)   |
| **3.0: WebSockets**               |   ⏳    | Implement full-duplex, bidirectional communication suitable for real-time interactive applications. | *(In Progress)*                                                                                                                                                                        |
| **4.0: gRPC**                     |   📋   | Implement a high-performance, contract-first RPC service using Protocol Buffers.                    | *(Planned)*                                                                                                                                                                            |
| **5.0: RSocket**                  |   📋   | Demonstrate a modern, reactive protocol with multiple, well-defined interaction models.             | *(Planned)*                                                                                                                                                                            |
| **6.0: Final Analysis**           |   📋   | Consolidate all benchmark results and write a final analysis to complete the project's narrative.   | *(Planned)*                                                                                                                                                                            |