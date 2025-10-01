#!/bin/bash
#
# Master benchmark orchestration script.
#
# This script orchestrates the entire benchmark suite, including pre- and
# post-load memory snapshots for every performance test to provide a
# comprehensive resource utilization profile.

set -e

# --- Make script path-independent ---
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PROJECT_ROOT=$( cd -- "${SCRIPT_DIR}/.." &> /dev/null && pwd )
cd "${PROJECT_ROOT}"

# --- Configuration ---
RESULTS_DIR="bench-clients/results/raw"
PROTOCOLS=("rest" "sse" "ws" "grpc")
RUNTIMES=("server-jvm" "server-native")
STABILIZATION_S=15

# --- Script Body ---
echo "🚀 Starting full benchmark suite from project root: ${PROJECT_ROOT}"
echo "-------------------------------------"
echo "Results will be saved in: ${RESULTS_DIR}"

# Ensure a completely clean state by stopping any running services first.
echo "   - Ensuring clean environment (docker compose down)..."
docker compose down -v --remove-orphans
echo "-------------------------------------"

# Ensure a clean slate for results
rm -rf "${RESULTS_DIR}"
mkdir -p "${RESULTS_DIR}"

# --- 1. Startup Benchmarks ---
echo ""
echo "▶️  Running Startup Benchmarks (services will be started and stopped)..."
echo "-------------------------------------"
for runtime in "${RUNTIMES[@]}"; do
    LOG_FILE="${RESULTS_DIR}/startup-${runtime}.log"
    BENCHMARK_SCRIPT="bench-clients/startup-benchmark.sh"

    echo "   - Measuring startup for ${runtime}..."
    echo "     (Output will be logged to ${LOG_FILE})"

    if ./"${BENCHMARK_SCRIPT}" "${runtime}" > "${LOG_FILE}"; then
        echo "     ✅  Successfully measured startup for ${runtime}."
    else
        echo "     ❌  Failed startup measurement for ${runtime}. Aborting."
        exit 1
    fi
done
echo "-------------------------------------"

# --- 2. Start Main Services for Load Testing ---
echo ""
echo "▶️  Starting main services for load testing (docker compose up)..."
# Build images if they don't exist, and start in detached mode.
docker compose up -d --build
echo "   - Waiting for services to initialize..."
sleep 10
echo "-------------------------------------"


# --- 3. Load Test Benchmarks ---
echo ""
echo "▶️  Running Load Test Benchmarks with Memory Snapshots..."
echo "-------------------------------------"

MEM_LOG_FILE="${RESULTS_DIR}/memory.log"
echo "protocol,runtime,state,value" > "${MEM_LOG_FILE}"

for protocol in "${PROTOCOLS[@]}"; do
    for runtime in "${RUNTIMES[@]}"; do
        echo "   - Starting benchmark for ${protocol} on ${runtime}..."

        echo "     - Capturing pre-load memory snapshot..."
        mem_usage_pre=$(docker stats "${runtime}" --no-stream --format "{{.MemUsage}}" | sed 's/MiB.*//')
        echo "${protocol},${runtime},pre,${mem_usage_pre}" >> "${MEM_LOG_FILE}"

        # Run the actual performance benchmark
        PERF_LOG_FILE="${RESULTS_DIR}/${protocol}-${runtime}.log"
        BENCHMARK_SCRIPT="bench-clients/${protocol}-benchmark.sh"
        echo "     - Applying load..."
        if ./"${BENCHMARK_SCRIPT}" "${runtime}" | tee "${PERF_LOG_FILE}"; then
            echo "     ✅  Load test completed."
        else
            echo "     ❌  Load test failed for ${protocol} on ${runtime}. Aborting."; exit 1;
        fi

        echo "     - Waiting ${STABILIZATION_S}s for post-load stabilization..."
        sleep ${STABILIZATION_S}
        echo "     - Capturing post-load memory snapshot..."
        mem_usage_post=$(docker stats "${runtime}" --no-stream --format "{{.MemUsage}}" | sed 's/MiB.*//')
        echo "${protocol},${runtime},post,${mem_usage_post}" >> "${MEM_LOG_FILE}"

        echo "   ✅  Benchmark for ${protocol} on ${runtime} fully complete."
        echo "-------------------------------------"
    done
done

echo ""
echo "🎉 Full benchmark suite completed successfully!"
echo "   - Cleaning up environment (docker compose down)..."
docker compose down -v --remove-orphans
echo ""
echo "Raw output files are located in ${PROJECT_ROOT}/${RESULTS_DIR}"