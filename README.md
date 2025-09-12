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

<details>
<summary>Click to see sample outputs</summary>

**Sample Output (JVM):**
```bash
--- Measuring startup time for service: server-jvm (5 runs) ---
[+] Running 2/2ment 1/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-jvm                               Started                                                                                                 0.1s 
Result: 351.000 ms
[+] Running 2/2ment 2/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-jvm                               Started                                                                                                 0.1s 
Result: 333.000 ms
[+] Running 2/2ment 3/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-jvm                               Started                                                                                                 0.1s 
Result: 344.000 ms
[+] Running 2/2ment 4/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-jvm                               Started                                                                                                 0.1s 
Result: 346.000 ms
[+] Running 2/2ment 5/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-jvm                               Started                                                                                                 0.1s 
Result: 328.000 ms
-----------------------------------------------------
Average startup time for 'server-jvm' (over 5 runs): 340.400 ms
-----------------------------------------------------
Measurement complete.
```

**Sample Output (Native):**
```bash
--- Measuring startup time for service: server-native (5 runs) ---
[+] Running 2/2ment 1/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-native                            Started                                                                                                 0.1s 
Result: 8.000 ms
[+] Running 2/2ment 2/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-native                            Started                                                                                                 0.1s 
Result: 8.000 ms
[+] Running 2/2ment 3/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-native                            Started                                                                                                 0.1s 
Result: 8.000 ms
[+] Running 2/2ment 4/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-native                            Started                                                                                                 0.1s 
Result: 8.000 ms
[+] Running 2/2ment 5/5... 
 ✔ Network quarkus-communication-patterns-lab_lab-network  Created                                                                                                 0.0s 
 ✔ Container quarkus-lab-native                            Started                                                                                                 0.1s 
Result: 7.000 ms
-----------------------------------------------------
Average startup time for 'server-native' (over 5 runs): 7.800 ms
-----------------------------------------------------
Measurement complete.
```
</details>

### 2. Run the REST Load Test

This script uses [`wrk`](https://github.com/wg/wrk) to measure the throughput and latency of the synchronous
`POST /echo` endpoint.

```bash
# Benchmark the JVM service's REST endpoint
./bench-clients/rest_benchmark.sh server-jvm

# Benchmark the Native service's REST endpoint
./bench-clients/rest_benchmark.sh server-native
```
<details>
<summary>Click to see sample output</summary>

```bash
--- Starting REST API Benchmark against server-jvm on network quarkus-communication-patterns-lab_lab-network ---
Running 30s test @ http://server-jvm:8080/echo
  4 threads and 50 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.50ms    4.86ms 144.99ms   98.61%
    Req/Sec    10.79k     3.06k   17.22k    75.65%
  1287387 requests in 30.10s, 211.17MB read
Requests/sec:  42775.71
Transfer/sec:      7.02MB
--- REST API Benchmark Complete ---
```
</details>

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

<details>
<summary>Click to see sample output</summary>

```bash
  █ THRESHOLDS 

    failed_connections
    ✓ 'count==0' count=0

    time_to_first_message
    ✓ 'p(95)<1500' p(95)=2.54ms


  █ TOTAL RESULTS 

    CUSTOM
    failed_connections......: 0      0/s
    messages_received.......: 4280   42.799481/s
    sse_event...............: 4280   42.799481/s
    time_to_first_message...: avg=1.02ms min=0s med=1ms max=7ms p(90)=2ms p(95)=2.54ms

    EXECUTION
    vus.....................: 1      min=1       max=50
    vus_max.................: 50     min=50      max=50

    NETWORK
    data_received...........: 373 kB 3.7 kB/s
    data_sent...............: 6.7 kB 67 B/s




running (1m40.0s), 00/50 VUs, 0 complete and 50 interrupted iterations
default ✓ [ 100% ] 01/50 VUs  1m10s
--- SSE Benchmark Complete ---
```
*Note: A warning about "interrupted iterations" may appear in the k6 output. This is an expected artifact of testing a long-running streaming protocol and does not indicate a failure.*
</details>

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