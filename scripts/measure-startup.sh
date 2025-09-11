#!/bin/bash

# This script measures the "time to readiness" for a specific service.
# It ensures the image is pre-built and then measures the time from "docker compose up"
# until the service's health check passes.

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
# The target service to measure. Passed as the first argument.
TARGET_SERVICE=$1

# --- Validation ---
if [ -z "$TARGET_SERVICE" ]; then
    echo "Error: No service name provided."
    echo "Usage: ./scripts/measure-startup.sh <service_name>"
    echo "Example: ./scripts/measure-startup.sh server-jvm"
    exit 1
fi

echo "--- Measuring startup time for service: $TARGET_SERVICE ---"

# --- Pre-computation Step: Ensure a clean state ---
echo "1. Ensuring all services are down..."
docker compose down -v --remove-orphans

# --- Pre-computation Step: Verify the image is built ---
# We separate build time from startup time. The script will fail if the image 
# is not already built.
echo "2. Verifying that the image for '$TARGET_SERVICE' is already built..."
IMAGE_NAME="quarkus-lab/${TARGET_SERVICE}:${TAG:-latest}"
if [[ -z "$(docker images -q "$IMAGE_NAME")" ]]; then
    echo "Error: Image '$IMAGE_NAME' not found."
    echo "Please build the image first. For example: 'docker compose build $TARGET_SERVICE'"
    exit 1
fi
echo "Image '$IMAGE_NAME' found."

# --- Configuration for Health Check ---
if [ "$TARGET_SERVICE" == "server-jvm" ]; then
    HEALTH_URL="http://localhost:8080/q/health/live"
elif [ "$TARGET_SERVICE" == "server-native" ]; then
    HEALTH_URL="http://localhost:8081/q/health/live"
else
    echo "Error: Unknown service '$TARGET_SERVICE'. Cannot determine health check URL."
    exit 1
fi

# --- Measurement Step ---
echo "3. Starting timer and launching service..."
# Record the start time with high precision (nanoseconds).
start_time=$(date +%s%N)

# Start the specific service and wait for its health check to pass.
# The '--no-deps' flag prevents it from starting other services, giving us an
# isolated measurement of the target service's startup time.
docker compose up -d --no-build --no-deps "$TARGET_SERVICE"
echo "Waiting for service to become healthy at ${HEALTH_URL}..."
until curl --output /dev/null --silent --head --fail "$HEALTH_URL"; do
    printf '.'
    sleep 0.5
done
echo

# Record the end time.
end_time=$(date +%s%N)
echo "Service is healthy."

# --- Reporting Step ---
echo "4. Calculating and reporting results..."
# Calculate the duration in nanoseconds.
duration_ns=$((end_time - start_time))
# Convert to milliseconds for readability.
duration_ms=$(echo "scale=3; $duration_ns / 1000000" | bc)

echo "-----------------------------------------------------"
echo "Startup time for '$TARGET_SERVICE': ${duration_ms} ms"
echo "-----------------------------------------------------"

# --- Cleanup Step ---
echo "5. Tearing down the service..."
docker compose down
echo "Measurement complete."