#!/bin/bash
#
# Generates all benchmark graphs using gnuplot.
#
# This script reads the summary.csv file, filters the data for each specific
# metric, and uses a gnuplot template to render the final PNG graphs.

set -e

# --- Preamble: Make script path-independent ---
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PROJECT_ROOT=$( cd -- "${SCRIPT_DIR}/.." &> /dev/null && pwd )
cd "${PROJECT_ROOT}"

# --- Configuration ---
SUMMARY_CSV="bench-clients/results/summary.csv"
PLOT_SCRIPT="docs/benchmarks/plot.gp"
OUTPUT_DIR="docs/benchmarks/graphs"

# --- Dependency Check ---
if ! command -v gnuplot &> /dev/null; then
    echo "❌ Error: gnuplot is not installed."
    echo "Please install gnuplot to generate graphs (e.g., 'brew install gnuplot' or 'sudo apt-get install gnuplot')."
    exit 1
fi

# --- Main Script Body ---
echo "📊 Generating benchmark graphs..."
mkdir -p "${OUTPUT_DIR}"

# --- Data Preparation ---
# Create a temporary file with a pivoted version of the CSV.
# This makes plotting JVM vs. Native side-by-side much easier in gnuplot.
# Input:  protocol,runtime,metric,value
# Output: metric,JVM_value,Native_value
PIVOTED_DATA_FILE=$(mktemp)
awk -F',' '
    NR > 1 {
        key = $3 FS $1;
        if ($2 == "server-jvm") {
            jvm[key] = $4;
        } else if ($2 == "server-native") {
            native[key] = $4;
        }
    }
    END {
        for (key in jvm) {
            print key "," jvm[key] "," native[key];
        }
    }
' "${SUMMARY_CSV}" > "${PIVOTED_DATA_FILE}"

# --- Plotting Function ---
generate_plot() {
    local metric_name=$1
    local chart_title=$2
    local y_axis_label=$3
    local output_filename=$4

    echo "   - Generating ${output_filename}..."

    local temp_plot_data
    temp_plot_data=$(mktemp)
    grep "^${metric_name}," "${PIVOTED_DATA_FILE}" | cut -d',' -f2- > "${temp_plot_data}"

    # Check if we have data to plot
    if [ ! -s "${temp_plot_data}" ]; then
        echo "     ⚠️  Warning: No data found for metric '${metric_name}'. Skipping graph."
        rm "${temp_plot_data}"
        return
    fi

    gnuplot \
        -e "data_file='${temp_plot_data}'" \
        -e "output_file='${OUTPUT_DIR}/${output_filename}'" \
        -e "title_text='${chart_title}'" \
        -e "ylabel_text='${y_axis_label}'" \
        "${PLOT_SCRIPT}"

    rm "${temp_plot_data}"
}

# --- Generate All Graphs ---
generate_plot "time_ms" "Startup time" "Time (ms) - Lower is Better" "startup-time.png"
generate_plot "requests_per_sec" "REST throughput" "Requests/sec - Higher is Better" "rest-throughput.png"
generate_plot "p95_latency_ms" "REST P95 latency" "Latency (ms) - Lower is Better" "rest-latency.png"
generate_plot "messages_per_sec" "gRPC throughput" "Messages/sec - Higher is Better" "grpc-throughput.png"
generate_plot "p99_latency_ms" "gRPC P99 latency" "Latency (ms) - Lower is Better" "grpc-latency.png"
generate_plot "total_messages_sent" "WebSocket throughput" "Total Messages Sent - Higher is Better" "ws-throughput.png"
generate_plot "max_active_streams" "SSE connection capacity" "Max Concurrent Streams - Higher is Better" "sse-capacity.png"
generate_plot "memory_post_mb" "Post-load memory usage" "Memory (MiB) - Lower is Better" "memory-post-load.png"

# --- Cleanup ---
rm "${PIVOTED_DATA_FILE}"

echo "✅ All graphs generated successfully in ${OUTPUT_DIR}/"