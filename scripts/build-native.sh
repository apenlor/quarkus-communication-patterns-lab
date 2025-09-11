#!/bin/bash

# This script provides a convenient way to build the Native container image.
# Exit immediately if a command exits with a non-zero status.
set -e

echo "Building the server-native service... This will take several minutes."
docker compose build server-native

echo "Build complete for server-native."