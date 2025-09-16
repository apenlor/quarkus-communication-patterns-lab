#!/bin/bash

# --- REST Benchmark Wrapper Script ---
#
# This script is a convenience wrapper for running the k6 REST benchmark.
# It tests the performance of the synchronous POST /echo endpoint, measuring
# request throughput and latency under load.
#
# The script validates the provided service name and then invokes the unified
# "run-k6-benchmark.sh" with the "rest" protocol.
#
# Usage:
#   ./bench-clients/rest-benchmark.sh <service_name>
#
# Parameters:
#   service_name: The target service container [server-jvm, server-native].

# --- Strict Mode ---
set -euo pipefail

if [ -z "${1:-}" ]; then
    echo "Error: No target service name provided." >&2
    echo "Usage: ./bench-clients/rest-benchmark.sh <service_name>" >&2
    echo "Example: ./bench-clients/rest-benchmark.sh server-jvm" >&2
    exit 1
fi

# Find the unified script in the same directory as this wrapper.
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
"${SCRIPT_DIR}/k6/run-k6-benchmark.sh" rest "$1"