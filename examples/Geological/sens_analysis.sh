# ../../gradlew run --args=". . sens_analysis"
# Error:
# * What went wrong:
# Project directory '/Users/lin236/Documents/GitHub/SemanticObjects_CSIRO/examples/Geological' is not part of the build defined by settings file '/Users/lin236/Documents/GitHub/SemanticObjects_CSIRO/settings.gradle'. If this is an unrelated build, it must have its own settings file.

# Running with kotlin command COM: didn't work - couldn't find the main class
#kotlin ./SensitivityAnalysis.kt . . sens_analysis

# java -jar sensitivity-analysis.jar [project_root] [smol_file_path] [output_dir] [optional_parameter_name]

java -jar sensitivity-analysis.jar . examples/Geological/simulate_onto_19Feb2025.smol sens_analysis

#The script accepts the following command-line arguments:

#1. `project_root` (optional): The root directory of your SMOL project. Defaults to the current directory.
#2. `smol_file_path` (optional): Path to the SMOL file to analyze. Defaults to "examples/Geological/simulate_onto.smol" relative to the project root.
#3. `output_dir` (optional): Directory to store the sensitivity analysis results. Defaults to "sensitivity_analysis_results" in the project root.
#4. `parameter_name` (optional): Name of a specific parameter to analyze. If provided, only this parameter will be analyzed. If omitted, all parameters will be analyzed.