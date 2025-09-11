#!/bin/bash
# ===================================================================
# Measures the "time to readiness" for a service in docker-compose.
#
# IMPORTANT: This script ASSUMES the Docker image for the service
# has already been built. It deliberately decouples build time
# from startup time.
#
# Usage: ./scripts/measure_startup.sh [service_name]
# Example: ./scripts/measure_startup.sh server-jvm
# ===================================================================

# --- Configuration ---
SERVICE_NAME=${1:-server-jvm}
READINESS_URL="http://localhost:8080/q/health/ready"
POLL_INTERVAL=0.1

# --- Pre-flight Checks ---
if ! command -v docker-compose &> /dev/null; then
    echo "Error: docker-compose is not available."
    exit 1
fi
if ! command -v bc &> /dev/null; then
    echo "Error: 'bc' (basic calculator) is not available."
    exit 1
fi

# Check if the image for the service exists. If not, fail with an error.
IMAGE_NAME=$(docker-compose config --services | grep ${SERVICE_NAME} | xargs -I {} docker-compose images {} | awk 'NR>1 {print $1":"$2}')
if [[ "$(docker images -q ${IMAGE_NAME} 2> /dev/null)" == "" ]]; then
  echo "❌ Error: Docker image for service '${SERVICE_NAME}' not found."
  echo "Please build it first by running: docker-compose build ${SERVICE_NAME}"
  exit 1
fi

echo "--- Measuring startup time for service: ${SERVICE_NAME} ---"
echo "Image: ${IMAGE_NAME}"

# --- Cleanup any previous runs for a cold start ---
echo "Ensuring clean state..."
docker-compose down -v --remove-orphans > /dev/null 2>&1

# --- Start Timer and Service ---
start_time=$(date +%s.%N)
# Use --no-build flag to explicitly prevent building.
docker-compose up -d --no-build "${SERVICE_NAME}"

# --- Poll for Readiness ---
echo "Polling ${READINESS_URL} for a 200 OK response..."
until $(curl --output /dev/null --silent --head --fail ${READINESS_URL}); do
    sleep ${POLL_INTERVAL}
done

# --- Stop Timer and Calculate Duration ---
end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)

# --- Report and Cleanup ---
echo ""
echo "✅ Service is ready!"
printf "Startup Time: %.3f seconds\n" ${duration}
echo ""
echo "--- Cleaning up containers ---"
docker-compose down -v --remove-orphans > /dev/null 2>&1