#!/bin/bash

# ==============================================================================
# Custom gRPC Benchmark Runner Script
#
# This script builds and runs our custom, multi-threaded Java gRPC benchmark
# client. It is designed to produce a high-fidelity load against the
# stateful BidiChat service and measure end-to-end broadcast latency.
#
# Usage:
#   ./bench-clients/grpc-benchmark.sh <service_name>
#
# Parameters:
#   service_name: The target service container [server-jvm, server-native].
# ==============================================================================

# --- Strict mode ---
set -euo pipefail

# --- Spinner ---
# This spinner will now only run if the script is in an interactive terminal.
spinner() {
    local chars="/-\\|"
    while :; do
        for (( i=0; i<${#chars}; i++ )); do
            sleep 0.1
            echo -en "${chars:$i:1} Running..." "\r" > /dev/tty
        done
    done
}

# --- Argument validation ---
if [ -z "${1:-}" ]; then
    echo "Error: No target service name provided." >&2
    echo "Usage: ./bench-clients/grpc-benchmark.sh <server-jvm|server-native>" >&2
    exit 1
fi
TARGET_SERVICE=$1

# --- Configuration ---
: "${CONCURRENCY:=50}"
: "${DURATION_SECONDS:=30}"

BENCHMARK_PROJECT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/grpc-bench-client"
JAR_NAME_PATTERN="grpc-bench-client-*.jar"
JAR_PATH_GLOB="${BENCHMARK_PROJECT_DIR}/target/${JAR_NAME_PATTERN}"

# --- Determine target host and port ---
TARGET_HOST="localhost"
TARGET_PORT=""

case "$TARGET_SERVICE" in
  server-jvm)
    TARGET_PORT="9001"
    ;;
  server-native)
    TARGET_PORT="9002"
    ;;
  *)
    echo "Error: Invalid service specified. Please use 'server-jvm' or 'server-native'." >&2
    exit 1
    ;;
esac

TARGET_SERVICE_UPPER=$(echo "$TARGET_SERVICE" | tr '[:lower:]' '[:upper:]')

echo "============================================================"
echo " Preparing Custom gRPC Benchmark for: ${TARGET_SERVICE_UPPER}"
echo " Concurrency:    $CONCURRENCY"
echo " Duration:       $DURATION_SECONDS seconds"
echo "============================================================"
echo

# --- Build ---
echo "Building benchmark client JAR..."
(cd "$BENCHMARK_PROJECT_DIR" && ./mvnw clean package -q -DskipTests)
echo "Build complete."
echo

JAR_PATH=$(find "${BENCHMARK_PROJECT_DIR}/target" -name "${JAR_NAME_PATTERN}" -not -name "original-*.jar")

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: Benchmark JAR not found at ${JAR_PATH_GLOB} after build." >&2
    exit 1
fi
if [ "$(echo "$JAR_PATH" | wc -l)" -ne 1 ]; then
    echo "Error: Ambiguous JAR file. Found multiple files:" >&2
    echo "$JAR_PATH" >&2
    exit 1
fi

# --- Execution ---
echo "Running benchmark..."

# The `[ -t 1 ]` expression returns true only when the script is run interactively.
# When output is piped to `tee` (as in `run-all-benchmarks.sh`), this is false.
if [ -t 1 ]; then
    spinner &
    SPINNER_PID=$!
    # Ensure the spinner is killed when the script exits for any reason.
    trap 'kill $SPINNER_PID 2>/dev/null; echo -en "\r\033[K" > /dev/tty' EXIT
fi

# The Java application's output (the clean summary) goes to standard output.
java -jar "$JAR_PATH" --quiet "$TARGET_HOST" "$TARGET_PORT" "$CONCURRENCY" "$DURATION_SECONDS"

# Cleanly stop the spinner if it was started.
if [ -n "${SPINNER_PID:-}" ]; then
    kill "$SPINNER_PID" 2>/dev/null
    trap - EXIT # Clear the trap
    echo -en "\r\033[K" > /dev/tty
fi

echo
echo "============================================================"
echo " Custom gRPC Benchmark for ${TARGET_SERVICE_UPPER} complete."
echo "============================================================"