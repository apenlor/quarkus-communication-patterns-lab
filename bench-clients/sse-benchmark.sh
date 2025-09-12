#!/bin/bash

set -e

# --- Validation ---
TARGET_SERVICE_NAME=$1
if [ -z "$TARGET_SERVICE_NAME" ]; then
    echo "Error: No target service name provided." >&2
    echo "Usage: ./bench-clients/sse-benchmark.sh <service_name>" >&2
    echo "Example (JVM): ./bench-clients/sse-benchmark.sh server-jvm" >&2
    echo "Example (Native): ./bench-clients/sse-benchmark.sh server-native" >&2
    exit 1
fi

# --- Configuration ---
BENCHMARK_IMAGE_NAME="quarkus-lab/k6-with-sse"
TARGET_URL="http://${TARGET_SERVICE_NAME}:8080/stream/ticker"
NETWORK_NAME=$(docker inspect --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' "quarkus-lab-${TARGET_SERVICE_NAME#server-}")

# --- Path resolution ---
# Determine the absolute path of the directory containing this script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
# The directory to be mounted is the "sse" subdirectory
MOUNT_DIR="${SCRIPT_DIR}/sse"

echo "--- Preparing SSE Benchmark for ${TARGET_SERVICE_NAME} ---"
echo "Found service on network: ${NETWORK_NAME}"

# --- Conditional Build ---
if [[ -z "$(docker images -q ${BENCHMARK_IMAGE_NAME}:latest)" ]]; then
    echo "Custom k6 image '${BENCHMARK_IMAGE_NAME}' not found. Building..."
    # The Docker build context is the directory containing the Dockerfile.
    docker build -t ${BENCHMARK_IMAGE_NAME} "${MOUNT_DIR}"
else
    echo "Custom k6 image '${BENCHMARK_IMAGE_NAME}' already exists. Skipping build."
fi

# --- Run the benchmark ---
echo "Running benchmark..."
docker run --rm -i \
  --network="${NETWORK_NAME}" \
  -v "${MOUNT_DIR}:/scripts" \
  -e TARGET_URL="${TARGET_URL}" \
  ${BENCHMARK_IMAGE_NAME} \
  run /scripts/sse-benchmark.js

echo "--- SSE Benchmark Complete ---"