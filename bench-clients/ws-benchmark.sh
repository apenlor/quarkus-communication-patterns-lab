#!/bin/bash

# --- WebSocket Benchmark Wrapper Script ---
#
# This script is a convenience wrapper for running the k6 WebSocket benchmark.
# It tests the server's performance in handling concurrent, bidirectional
# WebSocket connections, measuring connection rates and message round-trip times.
#
# The script validates the provided service name and then invokes the unified
# "run-k6-benchmark.sh" with the "ws" protocol.
#
# Usage:
#   ./bench-clients/ws-benchmark.sh <service_name>
#
# Parameters:
#   service_name: The target service container [server-jvm, server-native].

# --- Strict Mode ---
set -euo pipefail

if [ -z "${1:-}" ]; then
    echo "Error: No target service name provided." >&2
    echo "Usage: ./bench-clients/ws-benchmark.sh <service_name>" >&2
    echo "Example: ./bench-clients/ws-benchmark.sh server-jvm" >&2
    exit 1
fi

# Find the unified script in the same directory as this wrapper.
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
"${SCRIPT_DIR}/k6/run-k6-benchmark.sh" ws "$1"