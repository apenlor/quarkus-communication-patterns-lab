#!/bin/bash
# This script provides a convenient way to build the JVM container image.
# Exit immediately if a command exits with a non-zero status.
set -e

echo "Building the server-jvm service..."
docker compose build server-jvm

echo "Build complete for server-jvm."