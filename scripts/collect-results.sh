#!/bin/bash
#
# Consolidates all raw benchmark log files into a single summary CSV.
# This script is a pure parser; it does not execute any benchmarks itself.

set -e

# --- Make script path-independent ---
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PROJECT_ROOT=$( cd -- "${SCRIPT_DIR}/.." &> /dev/null && pwd )
cd "${PROJECT_ROOT}"

# --- Configuration ---
RAW_RESULTS_DIR="bench-clients/results/raw"
SUMMARY_CSV="bench-clients/results/summary.csv"

parse_perf_log() {
    local protocol=$1
    local runtime=$2
    local log_file=$3

    case "${protocol}" in
        rest)
            rps=$(grep 'http_reqs' "${log_file}" | awk '{sub("/s", "", $3); print $3}')
            p95_latency=$(grep 'http_req_duration' "${log_file}" | grep -o 'p(95)=[0-9.]*ms' | cut -d'=' -f2 | tr -d 'ms')
            echo "rest,${runtime},requests_per_sec,${rps}" >> "${SUMMARY_CSV}"
            echo "rest,${runtime},p95_latency_ms,${p95_latency}" >> "${SUMMARY_CSV}"
            ;;
        sse)
            max_vus=$(grep 'vus_max' "${log_file}" | awk '{print $2}')
            echo "sse,${runtime},max_active_streams,${max_vus}" >> "${SUMMARY_CSV}"
            ;;
        ws)
            msgs_sent=$(grep 'ws_msgs_sent' "${log_file}" | awk '{print $2}')
            echo "ws,${runtime},total_messages_sent,${msgs_sent}" >> "${SUMMARY_CSV}"
            ;;
        grpc)
            throughput=$(grep 'Throughput:' "${log_file}" | awk '{gsub(",", ".", $2); print $2}')
            throughput_int=$(printf "%.0f" "$throughput")
            p99_latency_us=$(grep 'p99:' "${log_file}" | awk '{print $NF}')
            p99_latency_ms=$(awk "BEGIN {printf \"%.2f\", ${p99_latency_us} / 1000}")
            echo "grpc,${runtime},messages_per_sec,${throughput_int}" >> "${SUMMARY_CSV}"
            echo "grpc,${runtime},p99_latency_ms,${p99_latency_ms}" >> "${SUMMARY_CSV}"
            ;;
    esac
}

# --- Script Body ---
echo "📊 Collecting and parsing benchmark results..."
echo "protocol,runtime,metric,value" > "${SUMMARY_CSV}"

# --- Main Parsing Loop ---
if [ ! -d "${RAW_RESULTS_DIR}" ] || [ -z "$(ls -A ${RAW_RESULTS_DIR})" ]; then
    echo "⚠️  Warning: Raw results directory is empty. Run './scripts/run-all-benchmarks.sh' first."
else
    echo "   - Parsing logs from ${RAW_RESULTS_DIR}..."

    UNIFIED_MEM_LOG="${RAW_RESULTS_DIR}/memory.log"
    if [ -f "${UNIFIED_MEM_LOG}" ]; then
        # Use awk to transform 'protocol,runtime,state,value' into the final CSV format.
        # It skips the header line (NR>1).
        awk -F',' 'NR>1 {print $1 "," $2 ",memory_" $3 "_mb," $4}' "${UNIFIED_MEM_LOG}" >> "${SUMMARY_CSV}"
    fi

    for log_file in "${RAW_RESULTS_DIR}"/*.log; do
        filename=$(basename "${log_file}" .log)
        protocol=$(echo "${filename}" | cut -d'-' -f1)

        case "${protocol}" in
            startup)
                runtime=$(echo "${filename}" | cut -d'-' -f2-)
                avg_startup_time=$(awk '/Average startup time/ {print $(NF-1)}' "${log_file}")
                echo "startup,${runtime},time_ms,${avg_startup_time}" >> "${SUMMARY_CSV}"
                ;;
            rest|sse|ws|grpc)
                runtime=$(echo "${filename}" | cut -d'-' -f2-)
                parse_perf_log "${protocol}" "${runtime}" "${log_file}"
                ;;
            memory)
                # This case is empty to ignore the memory.log file in this loop.
                ;;
        esac
    done
fi

echo "✅ Collection complete. Summary saved to ${PROJECT_ROOT}/${SUMMARY_CSV}"