# 2. Custom gRPC benchmark client

* **Status:** Accepted
* **Date:** 2025-09-23

## Context

During the implementation of Phase 4.0 (gRPC), a key deliverable was to benchmark the performance of a long-lived,
stateful, bidirectional chat stream (`BidiChat`).

1. **`ghz` Investigation:** This specialized gRPC benchmarking tool was evaluated first. We found that it is designed
   for unary or simple streaming RPCs, but its execution model does not support interacting with a long-running stream
   where the client must both send and receive asynchronous messages over a single, persistent connection.
2. **`k6` Investigation:** We then evaluated `k6`, our project's standard tool for REST and WebSocket testing. We found
   that while `k6` has gRPC support, it suffers from the same fundamental limitation: its execution model is based on
   discrete iterations (VUs) and is not designed to manage the state of thousands of persistent, interactive,
   bidirectional connections.

Both tools produced misleading `DeadlineExceeded` errors because they were attempting to apply a stateless,
request-response testing model to a stateful, stream-based application.

## Decision

We made the pivotal architectural decision to **a custom, multithreaded Java benchmark client from scratch.**

This was deemed the only way to generate a meaningful and accurate performance benchmark that correctly reflects the
application's intended use case. The custom client was designed with a `CountDownLatch` for thread synchronization, a
dedicated thread for each client connection, and integrated the `HdrHistogram` library for high-fidelity, low-overhead
latency measurement.

## Consequences

### Positive

* **Benchmark Accuracy:** The custom client provides a scientifically valid measurement of the gRPC stream's
  performance, as its execution model perfectly matches the application's logic.
* **Uncovered Critical Insights:** This accurate benchmark was instrumental in discovering the Out-Of-Memory (OOM) issue
  with the native executable under high load, leading to the subsequent decision to implement rate-limiting for a more
  realistic test.

### Negative

* **Increased Complexity:** The project now contains an additional standalone Java application (`grpc-bench-client`)
  that must be maintained.
* **Time Investment:** Significant development effort was required to design, build, and debug the custom client. This
  was a deliberate trade-off in favor of correctness.