# 1. Startup time measurement methodology

* **Status:** Accepted
* **Date:** 2025-09-12

## Context

During the implementation of Phase 2.0 (Server-Sent Events), a core requirement was to establish a definitive and
reproducible method for measuring the startup time of both the JVM and native executable containers. Initial attempts
involved using the host machine's clock to time the interval between `docker-compose up` and the first successful health
check response.

This approach was proven to be fundamentally flawed and unreliable due to:

1. **Race Conditions:** The host-based script was susceptible to timing inconsistencies, where the health check probe
   could fire at slightly different points in the container's lifecycle on each run.
2. **Clock Skew:** Comparing the host clock to events happening inside the container introduced the risk of clock skew,
   a common and difficult-to-diagnose problem in distributed systems.
3. **Environmental Dependency:** The measurement was dependent on the host machine's performance and scheduling, making
   it non-portable and scientifically unsound for creating a pure application-level benchmark.

## Decision

We made the architectural decision to **use the application's own log output as the single source of truth for measuring
startup time.**

The `measure-startup.sh` script was engineered to capture the logs from a container post-startup and parse the specific,
framework-emitted message that signals application readiness (e.g., "started in X.XXXs"). This creates a pure,
environment-agnostic metric that measures only the application's bootstrap time, from its own perspective.

## Consequences

### Positive

* **Reproducibility:** This method is highly reproducible across different host machines, as it eliminates host-side
  timing variables.
* **Accuracy:** The measurement is a direct, high-fidelity report from the application itself, representing the true
  time-to-readiness.
* **Portability:** The benchmark and its results are more portable and can be reliably compared, as the methodology is
  self-contained within the Docker environment.
* **Methodological Purity:** It correctly isolates the application's performance from the performance of the underlying
  orchestration and host system.

### Negative

* **Framework Coupling:** The log-parsing script is coupled to the specific log format of the Quarkus framework. An
  update to Quarkus that changes this message would require a corresponding update to the script. This is a minor and
  acceptable trade-off.