#!/bin/bash
# ===================================================================
# Quarkus REST API Benchmark Script
# ===================================================================
# This script runs a load test against the /echo endpoint using wrk.
# It runs wrk inside a Docker container to avoid local installation.

echo "--- Starting REST API Benchmark ---"

# --- Configuration ---
TARGET_URL="http://localhost:8080/echo"
THREADS=4
CONNECTIONS=50
DURATION="30s"

# --- Docker Command ---
# --network host: This is the crucial part for local testing. It makes the
# container share the host's network stack, so 'localhost' inside the
# container correctly points to the 'localhost' of your machine where
# the Quarkus app is running.
# -v: Mounts the local 'rest' directory (containing post.lua) into the
# container's /data directory.

docker run --rm --network host \
  --platform linux/amd64 \
  -v "$(pwd)/bench-clients/rest:/data" \
  ghcr.io/william-yeh/wrk \
    -t${THREADS} \
    -c${CONNECTIONS} \
    -d${DURATION} \
    -s /data/post.lua \
    ${TARGET_URL}

echo "--- REST API Benchmark Complete ---"