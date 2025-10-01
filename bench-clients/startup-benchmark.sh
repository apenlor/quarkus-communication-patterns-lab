#!/bin/bash
# ===================================================================
# "Time to Application Readiness" Measurement Script
# ===================================================================
#
# Purpose:
# This script provides a platform-agnostic measurement of
# the application's internal startup time.

# Exit immediately if any command fails, ensuring a "fail-fast" behavior.
set -e

# --- 1. Input Validation ---
# Ensure the script is called with a valid service name argument.
TARGET_SERVICE=$1
if [ -z "$TARGET_SERVICE" ]; then
    echo "Error: No service name provided." >&2
    echo "Usage: ./scripts/measure-startup.sh <service_name>" >&2
    echo "Example: ./scripts/measure-startup.sh server-jvm" >&2
    exit 1
fi

# --- 2. Configuration ---
RUN_COUNT=5 # Number of times to run the measurement for a stable average.

# --- 3. Pre-computation ---
echo "--- Measuring startup time for service: $TARGET_SERVICE ($RUN_COUNT runs) ---"

# Verify that the target Docker image has been built before proceeding.
# This cleanly separates build time from startup time.
IMAGE_NAME="quarkus-lab/${TARGET_SERVICE}:${TAG:-latest}"
if [[ -z "$(docker images -q "$IMAGE_NAME")" ]]; then
    >&2 echo "Error: Image '$IMAGE_NAME' not found. Please build it first."
    exit 1
fi

# Map the logical service name to the physical health check URL on the host.
if [ "$TARGET_SERVICE" == "server-jvm" ]; then HEALTH_URL="http://localhost:8080/q/health/live";
elif [ "$TARGET_SERVICE" == "server-native" ]; then HEALTH_URL="http://localhost:8081/q/health/live";
else >&2 echo "Error: Unknown service '$TARGET_SERVICE'."; exit 1; fi

# --- 4. Measurement Loop ---
total_duration_s=0
all_runs_successful=true
for i in $(seq 1 $RUN_COUNT); do
    echo -n "Running measurement ${i}/${RUN_COUNT}... "
    # Ensure a clean slate by tearing down any previous run.
    docker compose down -v --remove-orphans > /dev/null 2>&1

    # Start the service and wait until it is externally reachable.
    # We wait for the health check to pass to ensure the logs are complete.
    docker compose up -d --no-build --no-deps "$TARGET_SERVICE"
    until curl --output /dev/null --silent --head --fail "$HEALTH_URL"; do
        sleep 0.05
    done

    # This extracts just the numeric value of starting time log (e.g., "0.010").
    duration_s=$(docker compose logs "$TARGET_SERVICE" | grep 'started in' | sed 's/.*started in //;s/s.*//')

    # A defensive check to ensure the parsing was successful before performing math.
    if ! [[ "$duration_s" =~ ^[0-9.]+$ ]]; then
        echo "Error: Failed to parse a valid duration from logs."
        duration_s=0
        all_runs_successful=false
    fi

    printf "Result: %.3f ms\n" "$(echo "$duration_s * 1000" | bc)"
    total_duration_s=$(echo "$total_duration_s + $duration_s" | bc)
done

# --- 5. Reporting and Final Cleanup ---
if [ "$all_runs_successful" = true ]; then
    # Calculate the final average using `bc` for floating-point arithmetic.
    average_s=$(echo "scale=6; $total_duration_s / $RUN_COUNT" | bc)
    average_ms=$(echo "scale=3; $average_s * 1000" | bc)
    echo "-----------------------------------------------------"
    printf "Average startup time for '%s' (over %d runs): %.3f ms\n" "$TARGET_SERVICE" "$RUN_COUNT" "$average_ms"
    echo "-----------------------------------------------------"
else
    echo "-----------------------------------------------------"
    echo "One or more runs failed to parse. Average is not reliable."
    echo "-----------------------------------------------------"
fi

docker compose down -v --remove-orphans > /dev/null 2>&1
echo "Measurement complete."