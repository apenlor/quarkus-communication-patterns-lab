#!/bin/bash
# ===================================================================
# Quarkus REST API Benchmark Script
# ===================================================================
# This script runs a load test against the /echo endpoint using wrk.
# It runs wrk inside a Docker container to avoid local installation
# and ensure reproducibility.

set -e

# --- Validation and Configuration ---
TARGET_SERVICE_NAME=$1
if [ -z "$TARGET_SERVICE_NAME" ]; then
    echo "Error: No target service name provided." >&2
    echo "Usage: ./bench-clients/rest-benchmark.sh <service_name>" >&2
    echo "Example (JVM): ./bench-clients/rest-benchmark.sh server-jvm" >&2
    echo "Example (Native): ./bench-clients/rest-benchmark.sh server-native" >&2
    exit 1
fi

# Construct the full, internal URL that wrk will use.
# Docker's DNS will resolve the service name to its container IP.
# The internal port for Quarkus is always 8080.
TARGET_URL="http://${TARGET_SERVICE_NAME}:8080/echo"

# --- Dynamic Network Discovery ---
# Discover the network of the running target service to ensure wrk joins the same one.
CONTAINER_NAME="quarkus-lab-${TARGET_SERVICE_NAME#server-}"
NETWORK_NAME=$(docker inspect --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' "$CONTAINER_NAME")

if [ -z "$NETWORK_NAME" ]; then
    echo "Error: Could not discover the Docker network for container '${CONTAINER_NAME}'." >&2
    echo "Please ensure the application is running with 'docker compose up -d'." >&2
    exit 1
fi

echo "--- Starting REST API Benchmark against ${TARGET_SERVICE_NAME} on network ${NETWORK_NAME} ---"

# --- Robust Path Resolution ---
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
MOUNT_DIR="${SCRIPT_DIR}/rest"

# --- Docker Command ---
THREADS=4
CONNECTIONS=50
DURATION="30s"
# --network host: This is the crucial part for local testing. It makes the
# container share the host's network stack, so 'localhost' inside the
# container correctly points to the 'localhost' of your machine where
# the Quarkus app is running.
# -v: Mounts the local 'rest' directory (containing post.lua) into the
# container's /data directory.
docker run --rm \
  --network="${NETWORK_NAME}" \
  --platform linux/amd64 \
  -v "${MOUNT_DIR}:/data" \
  ghcr.io/william-yeh/wrk \
    -t${THREADS} \
    -c${CONNECTIONS} \
    -d${DURATION} \
    -s /data/post.lua \
     "${TARGET_URL}" &> wrk_output.log &

WRK_PID=$!
# --- Visual Feedback Spinner ---
spinner() {
    local i sp n
    sp='/-\|'
    n=${#sp}
    printf ' '
    while kill -0 $WRK_PID 2>/dev/null; do
        printf '\b%s' "${sp:i++%n:1}"
        sleep 0.1
    done
    printf '\b \b'
}

spinner

# Wait for the wrk process to finish and check its exit code
wait "$WRK_PID"
EXIT_CODE=$?

# Display the captured output
cat wrk_output.log
rm wrk_output.log

if [ $EXIT_CODE -ne 0 ]; then
    echo "--- REST API Benchmark FAILED ---"
    exit $EXIT_CODE
else
    echo "--- REST API Benchmark Complete ---"
fi