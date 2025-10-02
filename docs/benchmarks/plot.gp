# Gnuplot script for generating benchmark comparison bar charts.
# This script is designed to be called from a shell script, which
# provides the necessary variables (e.g., title, metric, output file).

# --- Terminal and Output Configuration ---
set terminal pngcairo size 800,600 enhanced font "Verdana,10"
set output output_file # This variable is passed from the shell script

# --- Chart Style ---
set title title_text font ",14"
set ylabel ylabel_text
set style data histograms
set style histogram cluster gap 1
set style fill solid border -1
set boxwidth 0.9
set key top left
set grid ytics
set yrange [0:*] # Ensure Y-axis starts at 0

# --- Data Source ---
# Use a comma as the data file separator
set datafile separator ","
# The 'data' variable will contain pre-filtered CSV data piped from the shell script
data_source = "< cat " . data_file

# --- Plot Command ---
# This is the core logic. It plots two columns of data from the pre-filtered CSV.
#   - using 2:xtic(1): For the first data series (JVM), use column 2 for the bar height
#     and column 1 for the X-axis tick label.
#   - using 3: Same for the second data series (Native).
#   - lt rgb "#4E79A7": Line type (lt) with a specific color for the JVM bars.
#   - lt rgb "#F28E2B": A different color for the Native bars.
plot data_source using 2:xtic(1) title "JVM" lt rgb "#4E79A7", \
     '' using 3 title "Native" lt rgb "#F28E2B"