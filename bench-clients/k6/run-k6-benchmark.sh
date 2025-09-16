#!/bin/bash

# --- Unified k6 Benchmark Runner ---
#
# This is the central script for executing all k6-based benchmarks in the project.
# It is called by protocol-specific wrapper scripts (e.g., rest-benchmark.sh).
#
# The script determines the correct Docker image, k6 script, and target URL based
# on the protocol, then dynamically discovers the container's network to run the test.
#
# DO NOT CALL THIS SCRIPT DIRECTLY. Use the wrappers.

# --- Strict Mode & Argument Parsing ---
set -euo pipefail

if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
    echo "Internal Error: This script requires two arguments: <protocol> <service_name>" >&2
    exit 1
fi
PROTOCOL=$1
TARGET_SERVICE_NAME=$2

# --- Protocol-Specific Configuration ---
K6_IMAGE=""
SCRIPT_SUBDIR=""
SCRIPT_NAME=""
TARGET_PROTOCOL=""
TARGET_PATH=""

case "$PROTOCOL" in
  rest)
    K6_IMAGE="grafana/k6:latest"
    SCRIPT_SUBDIR="rest"
    SCRIPT_NAME="rest-benchmark.js"
    TARGET_PROTOCOL="http"
    TARGET_PATH="/echo"
    ;;
  sse)
    K6_IMAGE="quarkus-lab/k6-with-sse"
    SCRIPT_SUBDIR="sse"
    SCRIPT_NAME="sse-benchmark.js"
    TARGET_PROTOCOL="http"
    TARGET_PATH="/stream/ticker"
    ;;
  ws)
    K6_IMAGE="grafana/k6:latest"
    SCRIPT_SUBDIR="websockets"
    SCRIPT_NAME="ws-benchmark.js"
    TARGET_PROTOCOL="ws"
    TARGET_PATH="/ws/chat"
    ;;
  *)
    echo "Internal Error: Unknown protocol '${PROTOCOL}'." >&2
    exit 1
    ;;
esac

# --- Common Logic ---
TARGET_URL="${TARGET_PROTOCOL}://${TARGET_SERVICE_NAME}:8080${TARGET_PATH}"

# Dynamically find the Docker network
NETWORK_NAME=$(docker inspect --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' "${TARGET_SERVICE_NAME}" 2>/dev/null || true)
if [ -z "$NETWORK_NAME" ]; then
    echo "Error: Could not find a network for container '${TARGET_SERVICE_NAME}'." >&2
    echo "Is the service running?" >&2
    exit 1
fi

# Path Resolution
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
MOUNT_DIR="${SCRIPT_DIR}/${SCRIPT_SUBDIR}"

if [[ ! -f "${MOUNT_DIR}/${SCRIPT_NAME}" ]]; then
    echo "Error: Benchmark script not found at ${MOUNT_DIR}/${SCRIPT_NAME}" >&2
    exit 1
fi

echo "--- Preparing $(echo "$PROTOCOL" | tr '[:lower:]' '[:upper:]') Benchmark for ${TARGET_SERVICE_NAME} ---"
echo "Target URL: ${TARGET_URL}"
echo "Found service on network: ${NETWORK_NAME}"
echo "----------------------------------------------------------------"

# --- Conditional Build Step (for SSE) ---
if [[ "$PROTOCOL" == "sse" ]]; then
    if [[ -z "$(docker images -q ${K6_IMAGE}:latest)" ]]; then
        echo "Custom k6 image '${K6_IMAGE}' not found. Building..."
        docker build -t ${K6_IMAGE} "${MOUNT_DIR}"
    else
        echo "Custom k6 image '${K6_IMAGE}' already exists. Skipping build."
    fi
fi

# --- Execution ---
echo "Running k6 benchmark..."
docker run --rm -i \
  --network="${NETWORK_NAME}" \
  -v "${MOUNT_DIR}:/scripts" \
  -e TARGET_URL="${TARGET_URL}" \
  "${K6_IMAGE}" \
  run "/scripts/${SCRIPT_NAME}"

echo "----------------------------------------------------------------"
echo "--- $(echo "$PROTOCOL" | tr '[:lower:]' '[:upper:]') Benchmark for ${TARGET_SERVICE_NAME} Complete ---"