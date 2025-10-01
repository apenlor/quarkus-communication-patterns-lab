# 3. Exclude RSocket implementation

* **Status:** Accepted
* **Date:** 2025-10-01

## Context

The original project roadmap slated Phase 5.0 for the implementation and benchmarking of the RSocket protocol.
Our investigation proceeded through several stages:

1. **Initial approach:** Attempted to use the `io.quarkiverse.rsocket` Quarkus extension, but it was
   unmaintained and abandoned.
2. **Manual implementation:** Pivoted to a manual implementation using the core `rsocket-java` libraries. This approach
   was successful on the standard JVM and passed all integration tests.
3. **Native compilation failure:** The manual implementation resulted in a series of intractable, low-level native
   compilation errors with GraalVM. Despite multiple attempts to resolve these with build-time configuration), the issue
   persisted, indicating a deep incompatibility between the library's reflective
   features and the constraints of native compilation.
4. **CLI client failure:** The standalone Java CLI client (mirroring the gRPC phase) was blocked by a
   low-level Netty DNS resolver conflict, which, while solvable, represented another significant deviation from a
   smooth implementation.

## Decision

We have made the decision to **exclude the RSocket implementation from this project** and move
directly to the final analysis phase.

The cumulative weight of the technical roadblocks—lack of a supported Quarkus extension, critical native build failures,
and significant client-side tooling friction—indicates that a robust demonstration of
RSocket on this specific technical stack is not currently feasible without compromising the project's core principles.

## Consequences

### Positive

* **Maintains project quality:** We avoid introducing a brittle, difficult-to-maintain, and JVM-only implementation into
  a repository intended to showcase good practices.
* **Preserves project principles:** Upholds our "No Shortcuts" principle by refusing to accept a solution that does not
  meet our standards for native compilation and reproducibility.
* **Focuses on value:** Allows us to redirect effort towards the final, high-value phase of the project: a comprehensive
  performance analysis of the successfully implemented protocols.

### Negative

* **Reduced scope:** The project will not include a direct performance comparison for RSocket, which was one of the
  initially planned communication patterns. The final analysis will be based on REST, SSE, WebSockets, and gRPC.