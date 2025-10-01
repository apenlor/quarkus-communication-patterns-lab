#!/bin/bash
#
# Master benchmark orchestration script.
#
# This script orchestrates the entire benchmark suite in a clean, reproducible
# sequence: it stops the environment, runs startup tests, restarts the main
# environment, and then runs all load tests.

set -e

# --- Make script path-independent ---
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PROJECT_ROOT=$( cd -- "${SCRIPT_DIR}/.." &> /dev/null && pwd )
cd "${PROJECT_ROOT}"

# --- Configuration ---
RESULTS_DIR="bench-clients/results/raw"
PROTOCOLS=("rest" "sse" "ws" "grpc")
RUNTIMES=("server-jvm" "server-native")

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
# A brief pause to ensure services are fully initialized and ready for traffic.
sleep 5
echo "-------------------------------------"


# --- 3. Load Test Benchmarks ---
echo ""
echo "▶️  Running Load Test Benchmarks..."
echo "-------------------------------------"
for protocol in "${PROTOCOLS[@]}"; do
    for runtime in "${RUNTIMES[@]}"; do
        LOG_FILE="${RESULTS_DIR}/${protocol}-${runtime}.log"
        BENCHMARK_SCRIPT="bench-clients/${protocol}-benchmark.sh"

        echo "   - Running ${protocol} benchmark for ${runtime}..."
        echo "     (Output will be logged to ${LOG_FILE})"

        if ./"${BENCHMARK_SCRIPT}" "${runtime}" | tee "${LOG_FILE}"; then
            echo "     ✅  Successfully completed ${protocol} benchmark for ${runtime}."
        else
            echo "     ❌  Failed ${protocol} benchmark for ${runtime}. Aborting."
            exit 1
        fi
        echo "-------------------------------------"
    done
done

echo ""
echo "🎉 Full benchmark suite completed successfully!"
echo "   - Cleaning up environment (docker compose down)..."
docker compose down -v --remove-orphans
echo ""
echo "Raw output files are located in ${PROJECT_ROOT}/${RESULTS_DIR}"