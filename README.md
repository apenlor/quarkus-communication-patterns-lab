# Quarkus & GraalVM Communication Patterns Lab (v6.0-final)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/33df58ded13c4bf39ef8bc99670b7570)](https://app.codacy.com/gh/apenlor/quarkus-communication-patterns-lab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CI Build Status](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/apenlor/quarkus-communication-patterns-lab/actions/workflows/ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/apenlor/quarkus-communication-patterns-lab)](https://github.com/apenlor/quarkus-communication-patterns-lab/releases/latest)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This README documents the state of the project at the **v6.0-final** tag. This phase concludes the project by
introducing a fully automated, end-to-end benchmark orchestration and data analysis suite.

The primary deliverable of this phase is a set of reproducible scripts that allow anyone to regenerate the
project's complete findings from raw performance data to final graphical report with just two commands. This includes
an automated methodology for capturing pre- and post-load memory usage for every communication pattern.

## Technology stack

- **Backend:** Java 21 & Quarkus 3.x
- **Runtimes:** JVM, GraalVM Native
- **Containerization:** Docker & Docker Compose
- **Benchmark Tooling:**
    - `k6` for HTTP-based protocols
    - Custom Java Benchmark Client for gRPC
- **Orchestration & Analysis:** Bash, `gnuplot`

---

## How to generate the final analysis

This phase provides the tooling to reproduce the project's complete set of benchmark results and visualizations.

### Prerequisites

- Docker and Docker Compose
- A shell environment (Bash, Zsh)
- `gnuplot` (`brew install gnuplot` or `sudo apt-get install gnuplot`)

### 1. Clone and check out the final tag

Ensure you are on the correct version of the code.

```bash
git clone https://github.com/apenlor/quarkus-communication-patterns-lab.git
cd quarkus-communication-patterns-lab
git checkout v6.0-final
```

### 2. Run the complete benchmark suite

This is the master orchestration script. It will perform all the following actions automatically:

1. Tear down any existing Docker environment.
2. Run the startup time benchmarks for both JVM and Native runtimes.
3. Start the main `docker-compose` services.
4. Execute all performance benchmarks (REST, SSE, WebSockets, gRPC) against both runtimes.
5. Capture pre- and post-load memory snapshots for every test.
6. Tear down the Docker environment when finished.

This command will take several minutes to complete.

```bash
./scripts/run-all-benchmarks.sh
```

Upon completion, the `bench-clients/results/raw/` directory will be fully populated with all log files.

### 3. Collect and parse all results

This script reads all the raw log files and consolidates them into a single, clean `summary.csv` file.

```bash
./scripts/collect-results.sh
```

The final dataset will be available at `bench-clients/results/summary.csv`.

### 4. Generate all graphs

This script reads the `summary.csv` file and uses `gnuplot` to generate a full set of professional bar charts
visualizing the results.

```bash
./scripts/generate-graphs.sh
```

All PNG graph files will be saved in `docs/benchmarks/graphs/`.

---

## Project roadmap (state at v6.0-final)

- ✅ **[Phase 1.0: The REST Baseline](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v1.0-rest)**
- ✅ **[Phase 2.0: Server-Sent Events](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v2.0-sse)**
- ✅ **[Phase 3.0: WebSockets](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v3.0-websockets)**
- ✅ **[Phase 4.0: gRPC](https://github.com/apenlor/quarkus-communication-patterns-lab/tree/v4.0-grpc)**
- ✅ **[Phase 5.0: RSocket (Excluded)](docs/adr/003-exclude-rsocket-implementation.md)**
- ✅ **Phase 6.0: Final Analysis** - *(This is the current and final phase)* 