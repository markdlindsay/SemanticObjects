# Instructions for Running the SMOL Geological Model Sensitivity Analysis Tool

## Overview
This document provides instructions for running the SensitivityAnalysis.kt script to conduct sensitivity analysis on the SMOL geological model parameters.

## Prerequisites
- Kotlin compiler (1.4 or later)
- JDK 11 or later
- The SMOL simulation project with its dependencies
- Bash shell environment (for Linux/Mac) or Git Bash (for Windows)

## Setup

1. Ensure that the SMOL project is properly set up and can be executed via the execute_case.sh script
2. Place the SensitivityAnalysis.kt file in a suitable location, e.g., the root of your SMOL project

## Compilation

You can compile the Kotlin script in two ways:

### Option 1: Using the Kotlin compiler directly
```
kotlinc SensitivityAnalysis.kt -include-runtime -d sensitivity-analysis.jar
```

### Option 2: Adding to your existing Gradle build
1. Copy the SensitivityAnalysis.kt file to your src/main/kotlin directory (adjust the package declaration if needed) COM: WHEN NEEDED?
2. Build your project using:
```
./gradlew build COM:THIS IS THE ROOT DIR?
```

## Running the Sensitivity Analysis

### Option 1: Running the compiled JAR (if compiled with kotlinc)
```
java -jar sensitivity-analysis.jar [project_root] [smol_file_path] [output_dir] [optional_parameter_name]
```

### Option 2: Running with kotlin command
```
kotlin SensitivityAnalysis.kt [project_root] [smol_file_path] [output_dir] [optional_parameter_name]
```

### Option 3: Running through Gradle (if added to your project) # COM: did this
```
./gradlew run --args="[project_root] [smol_file_path] [output_dir] [optional_parameter_name]"
```

## Command-line Arguments

The script accepts the following command-line arguments:

1. `project_root` (optional): The root directory of your SMOL project. Defaults to the current directory.
2. `smol_file_path` (optional): Path to the SMOL file to analyze. Defaults to "examples/Geological/simulate_onto.smol" relative to the project root.
3. `output_dir` (optional): Directory to store the sensitivity analysis results. Defaults to "sensitivity_analysis_results" in the project root.
4. `parameter_name` (optional): Name of a specific parameter to analyze. If provided, only this parameter will be analyzed. If omitted, all parameters will be analyzed.

## Examples

### Run full sensitivity analysis using defaults
```
kotlin SensitivityAnalysis.kt
```

### Run with custom paths
```
kotlin SensitivityAnalysis.kt /path/to/my/project /path/to/my/project/custom.smol /path/to/output
```

### Run analysis for a specific parameter only
```
kotlin SensitivityAnalysis.kt /path/to/my/project /path/to/my/project/simulate_onto.smol /path/to/output SHALE_SIZE
```

## Output

The script will generate the following outputs in the specified output directory:

1. Default simulation output in `default_simulation_output.txt`
2. Individual simulation outputs for each parameter value in `{parameter_name}_{value}_output.txt`
3. A comprehensive sensitivity analysis report in `sensitivity_analysis_report.md`
4. Parameter-specific reports if running individual parameter analyses in `{parameter_name}_sensitivity_report.md`

## Interpreting Results

After running the analysis, open the sensitivity_analysis_report.md file in a Markdown viewer to see:
- Baseline simulation results
- Detailed parameter-by-parameter sensitivity analysis
- Tables of simulation results for each parameter value
- Visualization recommendations

## Troubleshooting

If you encounter issues:

1. Ensure execute_case.sh has executable permissions: `chmod +x execute_case.sh`
2. Verify that the SMOL project builds and runs correctly on its own
3. Check that the output directory is writable
4. Look for error messages in the console output
5. Increase timeouts in the script if simulations are timing out (default is 15 minutes per simulation)

## Modifying Parameters for Analysis

To change which parameters are analyzed or their test values, modify the `parameterValues` map in the SensitivityAnalysis.kt file.