import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.time.LocalDate

class SmolSensitivityAnalysis(
    private val projectRoot: String,
    private val smolFilePath: String,
    private val outputDir: String
) {
    private val originalSmolFile = File(smolFilePath)
    private val originalContent = originalSmolFile.readText()
    private val executeCaseScript = "$projectRoot/execute_case.sh"

    // Original model parameters and their values
    private val originalParameters = mapOf(
        "SHALE_SIZE" to 40.0,
        "CHALK_SIZE" to 99.0,
        "SANDSTONE_SIZE" to 26.5,
        "BASE_TEMPERATURE" to 2.5,
        "TEMP_FACTOR" to 30.0,
        "HYDROCARBON_INCREMENT" to 100.0,
        "START_PAST" to 136.0,
        "CHECK_START" to -66.0,
        "DEPOSITION_DURATION" to 2.0,
        "DIV_LAYERS" to 31,
        "TOR_LAYERS" to 5,
        "EKOFISK_LAYERS" to 1,
        "CAP_LAYERS" to 1,
        "AB1_LAYERS" to 5,
        "AB2_LAYERS" to 26
    )

    // For each variable, define the values to test
    private val parameterValues = mapOf(
        "SHALE_SIZE" to listOf(20.0, 30.0, 40.0, 50.0, 60.0),
        "CHALK_SIZE" to listOf(50.0, 75.0, 99.0, 125.0, 150.0),
        "SANDSTONE_SIZE" to listOf(13.0, 19.5, 26.5, 33.0, 40.0),
        "BASE_TEMPERATURE" to listOf(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0),
        "TEMP_FACTOR" to listOf(15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0),
        "HYDROCARBON_INCREMENT" to listOf(50.0, 75.0, 100.0, 125.0, 150.0),
        "START_PAST" to listOf(100.0, 115.0, 130.0, 136.0, 145.0, 160.0),
        "CHECK_START" to listOf(-80.0, -75.0, -70.0, -66.0, -60.0, -55.0, -50.0),
        "DEPOSITION_DURATION" to listOf(1.0, 1.5, 2.0, 2.5, 3.0),
        "DIV_LAYERS" to listOf(20.0, 25.0, 31.0, 35.0, 40.0),
        "TOR_LAYERS" to listOf(3.0, 4.0, 5.0, 6.0, 7.0),
        "EKOFISK_LAYERS" to listOf(1.0, 2.0, 3.0),
        "CAP_LAYERS" to listOf(1.0, 2.0, 3.0),
        "AB1_LAYERS" to listOf(3.0, 4.0, 5.0, 6.0, 7.0),
        "AB2_LAYERS" to listOf(20.0, 23.0, 26.0, 29.0, 32.0)
    )

    private val results = mutableMapOf<String, MutableList<SimulationResult>>()

    init {
        // Create output directory if it doesn't exist
        File(outputDir).mkdirs()

        // Make the execute_case.sh script executable
        ProcessBuilder("chmod", "+x", executeCaseScript).start().waitFor()

        // Create a backup of the original SMOL file
        val backupPath = "$smolFilePath.backup"
        Files.copy(
            Paths.get(smolFilePath),
            Paths.get(backupPath),
            StandardCopyOption.REPLACE_EXISTING
        )
        println("Created backup of original SMOL file at $backupPath")
    }

    /**
     * Run sensitivity analysis for all parameters sequentially
     */
    fun runSequentialSensitivityAnalysis() {
        for ((paramName, values) in parameterValues) {
            println("Starting sensitivity analysis for parameter: $paramName")

            val paramResults = mutableListOf<SimulationResult>()
            for (value in values) {
                val paramValue = value
                println("  Testing $paramName = $paramValue")

                try {
                    // Modify the original file with the new parameter value
                    modifySmolFile(paramName, paramValue)

                    // Run the simulation and get results
                    val result = runSimulation(paramName, paramValue)
                    paramResults.add(result)

                    // Restore the original file after each run
                    restoreOriginalFile()
                } catch (e: Exception) {
                    println("Error running simulation for $paramName = $paramValue: ${e.message}")
                    e.printStackTrace()

                    // Make sure we restore the original file even if there's an error
                    restoreOriginalFile()
                }
            }

            results[paramName] = paramResults
            println("Completed sensitivity analysis for parameter: $paramName")
        }

        // Generate report after all tests
        generateReport()
    }

    /**
     * Run a single parameter's sensitivity analysis
     */
    fun runParameterAnalysis(paramName: String) {
        val values = parameterValues[paramName] ?: return

        println("Starting sensitivity analysis for parameter: $paramName")

        val paramResults = mutableListOf<SimulationResult>()
        for (value in values) {
            val paramValue = value
            println("  Testing $paramName = $paramValue")

            try {
                // Modify the original file with the new parameter value
                modifySmolFile(paramName, paramValue)

                // Run the simulation and get results
                val result = runSimulation(paramName, paramValue)
                paramResults.add(result)

                // Restore the original file after each run
                restoreOriginalFile()
            } catch (e: Exception) {
                println("Error running simulation for $paramName = $paramValue: ${e.message}")
                e.printStackTrace()

                // Make sure we restore the original file even if there's an error
                restoreOriginalFile()
            }
        }

        results[paramName] = paramResults
        println("Completed sensitivity analysis for parameter: $paramName")

        // Generate report for this parameter only
        generateParameterReport(paramName)
    }

    /**
     * Run a single simulation with modified parameter
     */
    private fun runSimulation(
        paramName: String,
        paramValue: Double
    ): SimulationResult {
        val outputFile = File("$outputDir/${paramName}_${paramValue}_output.txt")
        val errorFile = File("$outputDir/${paramName}_${paramValue}_error.txt")

        val startTime = System.currentTimeMillis()

        // Create a modified execute_case.sh that uses the specific SMOL file
        val tempScriptPath = createTemporaryExecuteScript(smolFilePath)

        // Run the execute_case.sh script and capture output
        val process = ProcessBuilder("bash", tempScriptPath)
            .redirectOutput(outputFile)
            .redirectError(errorFile)
            .start()

        // Wait for the process to complete with a timeout
        val completed = process.waitFor(15, TimeUnit.MINUTES)

        val executionTime = System.currentTimeMillis() - startTime

        // Delete the temporary script
        File(tempScriptPath).delete()

        if (!completed) {
            process.destroy()
            return SimulationResult(
                paramName = paramName,
                paramValue = paramValue,
                trapCount = -1,
                leakCount = -1,
                migrationCount = -1,
                maturationsCount = -1,
                maxTemperature = -1.0,
                executionTime = executionTime,
                status = "TIMEOUT",
                finalTime = 0.0,
                finalDepth = 0.0,
                simulationSteps = 0,
                lastRockLayerSize = 0.0,
                typeErrors = 0,
                warnings = 0,
                executionSuccess = false
            )
        }

        // Parse the output file to collect metrics
        val metrics = parseOutputFile(outputFile.path)

        return SimulationResult(
            paramName = paramName,
            paramValue = paramValue,
            trapCount = metrics.trapCount,
            leakCount = metrics.leakCount,
            migrationCount = metrics.migrationCount,
            maturationsCount = metrics.maturationsCount,
            maxTemperature = metrics.maxTemperature,
            executionTime = executionTime,
            status = if (metrics.executionSuccess) "SUCCESS" else "FAILED",
            finalTime = metrics.finalTime,
            finalDepth = metrics.finalDepth,
            simulationSteps = metrics.simulationSteps,
            lastRockLayerSize = metrics.lastRockLayerSize,
            typeErrors = metrics.typeErrors,
            warnings = metrics.warnings,
            executionSuccess = metrics.executionSuccess
        )
    }

    /**
     * Create a temporary execute script that points to the specific SMOL file
     */
    private fun createTemporaryExecuteScript(smolFilePath: String): String {
        val originalScript = File(executeCaseScript).readText()
        val smolRelativePath = smolFilePath.removePrefix("$projectRoot/")

        // Modify the script to use the specific SMOL file
        val modifiedScript = originalScript.replace(
            "examples/Geological/simulate_onto.smol",
            smolRelativePath
        )

        // Write to a temporary script file
        val tempScriptPath = "$outputDir/temp_execute_${System.currentTimeMillis()}.sh"
        File(tempScriptPath).writeText(modifiedScript)

        // Make the temporary script executable
        ProcessBuilder("chmod", "+x", tempScriptPath).start().waitFor()

        return tempScriptPath
    }

    /**
     * Modify the SMOL file with a new parameter value
     */
    private fun modifySmolFile(paramName: String, paramValue: Double, filePath: String = smolFilePath) {
        val file = File(filePath)
        val content = file.readText()
        val modifiedContent = when (paramName) {
            "SHALE_SIZE" -> replaceShaleSize(content, paramValue)
            "CHALK_SIZE" -> replaceChalkSize(content, paramValue)
            "SANDSTONE_SIZE" -> replaceSandstoneSize(content, paramValue)
            "BASE_TEMPERATURE" -> replaceBaseTemperature(content, paramValue)
            "TEMP_FACTOR" -> replaceTempFactor(content, paramValue)
            "HYDROCARBON_INCREMENT" -> replaceHydrocarbonIncrement(content, paramValue)
            "START_PAST" -> replaceStartPast(content, paramValue)
            "CHECK_START" -> replaceCheckStart(content, paramValue)
            "DEPOSITION_DURATION" -> replaceDepositionDuration(content, paramValue)
            "DIV_LAYERS", "TOR_LAYERS", "EKOFISK_LAYERS", "CAP_LAYERS", "AB1_LAYERS", "AB2_LAYERS" ->
                replaceLayers(content, paramName, paramValue.toInt())
            else -> content
        }

        file.writeText(modifiedContent)
    }

    /**
     * Restore the original SMOL file
     */
    private fun restoreOriginalFile() {
        originalSmolFile.writeText(originalContent)
    }

    // Parameter modification helper methods
    private fun replaceShaleSize(content: String, newValue: Double): String {
        // Replace the initial ShaleUnit size parameter (mandal)
        return content.replace(
            "ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, 40.0,",
            "ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, $newValue,"
        )
    }

    private fun replaceChalkSize(content: String, newValue: Double): String {
        // Replace ChalkUnit size parameters
        return content.replace(
            "SandstoneUnit ekofisk = new ChalkUnit(null, null, null, null, null, null, 99.0,",
            "SandstoneUnit ekofisk = new ChalkUnit(null, null, null, null, null, null, $newValue,"
        )
    }

    private fun replaceSandstoneSize(content: String, newValue: Double): String {
        // Replace SandstoneUnit size parameter (div)
        return content.replace(
            "SandstoneUnit div = new SandstoneUnit(null, null, null, null, null, null, 26.5,",
            "SandstoneUnit div = new SandstoneUnit(null, null, null, null, null, null, $newValue,"
        )
    }

    private fun replaceBaseTemperature(content: String, newValue: Double): String {
        // Replace base temperature value in ShaleUnit update method
        return content.replace(
            "this.temperature = 2.5 + ((under/1000) * 30);",
            "this.temperature = $newValue + ((under/1000) * 30);"
        )
    }

    private fun replaceTempFactor(content: String, newValue: Double): String {
        // Replace temperature factor in ShaleUnit update method
        return content.replace(
            "this.temperature = 2.5 + ((under/1000) * 30);",
            "this.temperature = 2.5 + ((under/1000) * $newValue);"
        )
    }

    private fun replaceHydrocarbonIncrement(content: String, newValue: Double): String {
        // Replace hydrocarbon increment rate in ChalkUnit and SandstoneUnit
        var modified = content.replace(
            "if next + 100.0 < this.size then",
            "if next + $newValue < this.size then"
        )
        return modified.replace(
            "l.content = next + 100;",
            "l.content = next + $newValue;"
        )
    }

    private fun replaceStartPast(content: String, newValue: Double): String {
        // Replace startPast parameter in the simulation
        return content.replace(
            "driver.sim(dl, 136.0, mandal, (-66.0));",
            "driver.sim(dl, $newValue, mandal, (-66.0));"
        )
    }

    private fun replaceCheckStart(content: String, newValue: Double): String {
        // Replace checkStart parameter in the simulation
        return content.replace(
            "driver.sim(dl, 136.0, mandal, (-66.0));",
            "driver.sim(dl, 136.0, mandal, ($newValue));"
        )
    }

    private fun replaceDepositionDuration(content: String, newValue: Double): String {
        // Replace duration for all DepositionGenerators
        return content.replace(
            "DepositionGenerator dep = new DepositionGenerator(div, 2.0,",
            "DepositionGenerator dep = new DepositionGenerator(div, $newValue,"
        ).replace(
            "DepositionGenerator depTor = new DepositionGenerator(tor, 2.0,",
            "DepositionGenerator depTor = new DepositionGenerator(tor, $newValue,"
        ).replace(
            "DepositionGenerator depEko = new DepositionGenerator(ekofisk, 2.0,",
            "DepositionGenerator depEko = new DepositionGenerator(ekofisk, $newValue,"
        ).replace(
            "DepositionGenerator depCap = new DepositionGenerator(cap, 2.0,",
            "DepositionGenerator depCap = new DepositionGenerator(cap, $newValue,"
        ).replace(
            "DepositionGenerator depAb1 = new DepositionGenerator(ab1, 2.0,",
            "DepositionGenerator depAb1 = new DepositionGenerator(ab1, $newValue,"
        ).replace(
            "DepositionGenerator depAb2 = new DepositionGenerator(ab2, 2.0,",
            "DepositionGenerator depAb2 = new DepositionGenerator(ab2, $newValue,"
        )
    }

    private fun replaceLayers(content: String, layerType: String, newValue: Int): String {
        // Replace number of layers for a specific DepositionGenerator
        return when (layerType) {
            "DIV_LAYERS" -> content.replace(
                "DepositionGenerator dep = new DepositionGenerator(div, 2.0, 31);",
                "DepositionGenerator dep = new DepositionGenerator(div, 2.0, $newValue);"
            )
            "TOR_LAYERS" -> content.replace(
                "DepositionGenerator depTor = new DepositionGenerator(tor, 2.0, 5);",
                "DepositionGenerator depTor = new DepositionGenerator(tor, 2.0, $newValue);"
            )
            "EKOFISK_LAYERS" -> content.replace(
                "DepositionGenerator depEko = new DepositionGenerator(ekofisk, 2.0, 1);",
                "DepositionGenerator depEko = new DepositionGenerator(ekofisk, 2.0, $newValue);"
            )
            "CAP_LAYERS" -> content.replace(
                "DepositionGenerator depCap = new DepositionGenerator(cap, 2.0, 1);",
                "DepositionGenerator depCap = new DepositionGenerator(cap, 2.0, $newValue);"
            )
            "AB1_LAYERS" -> content.replace(
                "DepositionGenerator depAb1 = new DepositionGenerator(ab1, 2.0, 5);",
                "DepositionGenerator depAb1 = new DepositionGenerator(ab1, 2.0, $newValue);"
            )
            "AB2_LAYERS" -> content.replace(
                "DepositionGenerator depAb2 = new DepositionGenerator(ab2, 2.0, 26);",
                "DepositionGenerator depAb2 = new DepositionGenerator(ab2, 2.0, $newValue);"
            )
            else -> content
        }
    }

    /**
     * Parse the output file to extract key metrics
     */
    private fun parseOutputFile(outputFilePath: String): OutputMetrics {
        val file = File(outputFilePath)
        if (!file.exists()) {
            return OutputMetrics(
                trapCount = 0,
                leakCount = 0,
                migrationCount = 0,
                maturationsCount = 0,
                maxTemperature = 0.0,
                finalTime = 0.0,
                finalDepth = 0.0,
                buildSuccess = false,
                simulationSteps = 0,
                lastRockLayerSize = 0.0,
                typeErrors = 0,
                warnings = 0,
                executionSuccess = false
            )
        }

        val content = file.readText()

        // Count occurrences of specific events
        val trapCount = "trap".toRegex().findAll(content).count()
        val leakCount = "leak".toRegex().findAll(content).count()
        val migrationCount = "migrate from shale".toRegex().findAll(content).count()
        val maturationsCount = ">> maturation on-going!".toRegex().findAll(content).count()

        // Extract temperature data
        val tempPattern = ">> temp\\s*\\n(\\d+\\.\\d+)".toRegex()
        val temperatures = tempPattern.findAll(content)
            .map { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
            .toList()

        val maxTemperature = if (temperatures.isEmpty()) 0.0 else temperatures.maxOrNull() ?: 0.0

        // Extract the final simulation time
        val finalTimePattern = "> Ending simulation with t =-\\s*\\n([-]?\\d+\\.\\d+)".toRegex()
        val finalTimeMatch = finalTimePattern.find(content)
        val finalTime = finalTimeMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        // Extract the total number of simulation steps
        val simulationSteps = "> Updating simulation with t =".toRegex().findAll(content).count()

        // Extract depth data
        val depthPattern = ">> depth\\s*\\n(\\d+\\.\\d+)".toRegex()
        val depths = depthPattern.findAll(content)
            .map { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
            .toList()

        val finalDepth = if (depths.isEmpty()) 0.0 else depths.lastOrNull() ?: 0.0

        // Extract the final rock layer size
        val lastLayerSizePattern = "Sandstone at: \\s*\\n(\\d+\\.\\d+)".toRegex()
        val lastLayerSizes = lastLayerSizePattern.findAll(content)
            .map { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
            .toList()

        val lastRockLayerSize = if (lastLayerSizes.isEmpty()) 0.0 else lastLayerSizes.lastOrNull() ?: 0.0

        // Extract build and execution success
        val buildSuccess = "BUILD SUCCESSFUL".toRegex().containsMatchIn(content)

        // Extract type errors and warnings
        val typeErrors = "ERROR:".toRegex().findAll(content).count()
        val warnings = "WARNING:".toRegex().findAll(content).count()

        // Check if execution was successful
        val executionSuccess = "Ending simulation with t".toRegex().containsMatchIn(content)

        return OutputMetrics(
            trapCount = trapCount,
            leakCount = leakCount,
            migrationCount = migrationCount,
            maturationsCount = maturationsCount,
            maxTemperature = maxTemperature,
            finalTime = finalTime,
            finalDepth = finalDepth,
            buildSuccess = buildSuccess,
            simulationSteps = simulationSteps,
            lastRockLayerSize = lastRockLayerSize,
            typeErrors = typeErrors,
            warnings = warnings,
            executionSuccess = executionSuccess
        )
    }

    /**
     * Generate a comprehensive report of the sensitivity analysis
     */
    private fun generateReport() {
        val reportFile = File("$outputDir/sensitivity_analysis_report.md")

        reportFile.printWriter().use { writer ->
            writer.println("# Geological Model Sensitivity Analysis Report")
            writer.println("\nDate: ${LocalDate.now()}")

            // Summary statistics section based on default run
            val defaultFile = File("$outputDir/default_simulation_output.txt")
            if (defaultFile.exists()) {
                val defaultMetrics = parseOutputFile(defaultFile.absolutePath)
                writer.println("\n## Baseline Simulation Results\n")
                writer.println("The default simulation with original parameter values produced:")
                writer.println("* **Hydrocarbon Traps**: ${defaultMetrics.trapCount}")
                writer.println("* **Hydrocarbon Leaks**: ${defaultMetrics.leakCount}")
                writer.println("* **Shale Migrations**: ${defaultMetrics.migrationCount}")
                writer.println("* **Kerogen Maturations**: ${defaultMetrics.maturationsCount}")
                writer.println("* **Maximum Temperature**: ${String.format("%.2f", defaultMetrics.maxTemperature)}°C")
                writer.println("* **Final Depth**: ${String.format("%.2f", defaultMetrics.finalDepth)} units")
                writer.println("* **Simulation Steps**: ${defaultMetrics.simulationSteps}")
                writer.println("* **Final Time**: ${defaultMetrics.finalTime}")
                writer.println("* **Final Rock Layer Size**: ${defaultMetrics.lastRockLayerSize}")
                writer.println("* **Build Errors**: ${defaultMetrics.typeErrors}")
                writer.println("* **Build Warnings**: ${defaultMetrics.warnings}")
            }

            writer.println("\n## Sensitivity Analysis Results\n")

            for ((paramName, paramResults) in results) {
                writer.println("### Parameter: $paramName\n")

                // Create a comprehensive results table
                writer.println("| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |")
                writer.println("|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|")

                for (result in paramResults) {
                    writer.println(
                        "| ${result.paramValue} | ${result.trapCount} | ${result.leakCount} | " +
                                "${result.migrationCount} | ${result.maturationsCount} | " +
                                "${String.format("%.1f", result.maxTemperature)} | ${
                                    String.format(
                                        "%.1f",
                                        result.finalDepth
                                    )
                                } | " +
                                "${result.simulationSteps} | ${String.format("%.1f", result.lastRockLayerSize)} | " +
                                "${result.typeErrors + result.warnings} | ${result.executionTime} | ${result.status} |"
                    )
                }

                writer.println("\n#### Analysis\n")
                writer.println("* **Parameter**: ${describeParameter(paramName)}")
                writer.println("* **Range Tested**: ${parameterValues[paramName]}")
                writer.println("* **Original Value**: ${originalParameters[paramName]}")

                // Calculate and display parameter sensitivity
                if (paramResults.any { it.status == "SUCCESS" }) {
                    val successfulResults = paramResults.filter { it.status == "SUCCESS" }

                    // Find baseline result (result with original parameter value)
                    val baselineResult = successfulResults.firstOrNull {
                        it.paramValue == originalParameters[paramName]
                    }

                    if (baselineResult != null) {
                        writer.println("\n* **Sensitivity Analysis**:")
                        calculateSensitivity(writer, successfulResults, baselineResult)
                    }
                }

                writer.println("\n")
            }

            // Generate charts section (instructions for post-processing)
            writer.println("## Visualization Recommendations\n")
            writer.println("For better visualization of the sensitivity analysis results, we recommend creating the following charts:")
            writer.println("1. **Parameter vs. Trap Count**: Bar charts showing how trap count varies with parameter values")
            writer.println("2. **Parameter vs. Max Temperature**: Line charts showing temperature response curves")
            writer.println("3. **Parameter vs. Migration Events**: Trend analysis of hydrocarbon migration patterns")
            writer.println("4. **3D Surface Plot**: Showing parameter interactions (e.g., temperature factor vs. shale size vs. trap count)")
            writer.println("\nThese visualizations can be generated from the tabular data above using tools like Excel, Python (matplotlib/seaborn), or R.")

            // Generate overall conclusions
            writer.println("\n## Overall Conclusions\n")
            writer.println("Based on the sensitivity analysis, the parameters with the most significant impact on the model outcomes are:")

            // Define weight factors for different metrics based on geological importance
            val metricWeights = mapOf(
                "trapCount" to 1.0,        // Hydrocarbon traps are key petroleum system indicators
                "leakCount" to 0.8,        // Leaks are important but slightly less than traps
                "migrationCount" to 0.9,   // Migration is crucial for petroleum system
                "maturationsCount" to 1.0, // Maturation is fundamental to source rock productivity
                "maxTemperature" to 0.7,   // Temperature affects maturation but is an intermediate factor
                "finalDepth" to 0.5,       // Basin depth is important for context
                "simulationSteps" to 0.3   // Less geologically significant
            )

            // Calculate the overall sensitivity for each parameter with weighted metrics
            val parameterSensitivity = mutableMapOf<String, Double>()
            for ((paramName, paramResults) in results) {
                val successfulResults = paramResults.filter { it.status == "SUCCESS" }
                if (successfulResults.isNotEmpty()) {
                    val baselineResult = successfulResults.firstOrNull {
                        it.paramValue == originalParameters[paramName]
                    } ?: successfulResults.first()

                    val trapSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.trapCount } * (metricWeights["trapCount"] ?: 1.0)
                    val leakSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.leakCount } * (metricWeights["leakCount"] ?: 1.0)
                    val migrationSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.migrationCount } * (metricWeights["migrationCount"] ?: 1.0)
                    val maturationSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.maturationsCount } * (metricWeights["maturationsCount"] ?: 1.0)
                    val tempSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.maxTemperature.toInt() } * (metricWeights["maxTemperature"] ?: 1.0)
                    val depthSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.finalDepth.toInt() } * (metricWeights["finalDepth"] ?: 1.0)
                    val stepsSensitivity = calculateSensitivityValue(
                        successfulResults,
                        baselineResult
                    ) { it.simulationSteps } * (metricWeights["simulationSteps"] ?: 1.0)

                    // Combined weighted sensitivity score
                    val score = trapSensitivity + leakSensitivity + migrationSensitivity + maturationSensitivity +
                            tempSensitivity + depthSensitivity + stepsSensitivity

                    parameterSensitivity[paramName] = score
                }
            }

            // Sort parameters by sensitivity and list the top parameters
            val sortedParameters = parameterSensitivity.entries
                .sortedByDescending { it.value }
                .take(7)

            // Create a sensitivity ranking table
            writer.println("\n| Rank | Parameter | Sensitivity Score | Primary Impact |")
            writer.println("|------|-----------|-------------------|----------------|")

            sortedParameters.forEachIndexed { index, (param, value) ->
                writer.println(
                    "| ${index + 1} | **$param** | ${String.format("%.3f", value)} | ${
                        describeSensitivity(
                            param
                        )
                    } |"
                )
            }

            // List key takeaways
            writer.println("\n### Key Takeaways\n")

            // Most sensitive parameter insights
            val mostSensitiveParam = sortedParameters.firstOrNull()?.key ?: ""
            if (mostSensitiveParam.isNotEmpty()) {
                writer.println(
                    "1. **${mostSensitiveParam}** is the most influential parameter in the model, with primary impact on ${
                        getParameterPrimaryImpact(
                            mostSensitiveParam
                        )
                    }"
                )

                // Group parameters by type
                val temperatureParams = sortedParameters.filter { it.key in listOf("BASE_TEMPERATURE", "TEMP_FACTOR") }
                val rockSizeParams =
                    sortedParameters.filter { it.key in listOf("SHALE_SIZE", "CHALK_SIZE", "SANDSTONE_SIZE") }
                val timingParams =
                    sortedParameters.filter { it.key in listOf("START_PAST", "CHECK_START", "DEPOSITION_DURATION") }
                val layerParams = sortedParameters.filter {
                    it.key in listOf(
                        "DIV_LAYERS",
                        "TOR_LAYERS",
                        "EKOFISK_LAYERS",
                        "CAP_LAYERS",
                        "AB1_LAYERS",
                        "AB2_LAYERS"
                    )
                }

                // Add insights based on parameter groupings
                if (temperatureParams.isNotEmpty()) {
                    writer.println("2. **Temperature parameters** (${temperatureParams.joinToString(", ") { it.key }}) show ${if (temperatureParams.any { it.value > 1.0 }) "high" else "moderate"} sensitivity, indicating the importance of thermal modeling in this geological system")
                }

                if (rockSizeParams.isNotEmpty()) {
                    writer.println("3. **Rock unit dimensions** (${rockSizeParams.joinToString(", ") { it.key }}) have ${if (rockSizeParams.any { it.value > 1.0 }) "significant" else "moderate"} impact on the petroleum system outcomes")
                }

                if (timingParams.isNotEmpty()) {
                    writer.println("4. **Timing parameters** (${timingParams.joinToString(", ") { it.key }}) show ${if (timingParams.any { it.value > 1.0 }) "high" else "moderate"} sensitivity, highlighting the importance of geological history in hydrocarbon generation")
                }

                if (layerParams.isNotEmpty()) {
                    writer.println("5. **Layer count parameters** (${layerParams.joinToString(", ") { it.key }}) demonstrate ${if (layerParams.any { it.value > 1.0 }) "significant" else "moderate"} influence on basin development and hydrocarbon system evolution")
                }
            }
            println("Sensitivity analysis report generated at: ${reportFile.absolutePath}")
        }
    }

    /**
     * Generate a report for a single parameter
     */
    private fun generateParameterReport(paramName: String) {
        val paramResults = results[paramName] ?: return
        val reportFile = File("$outputDir/${paramName}_sensitivity_report.md")

        reportFile.printWriter().use { writer ->
            writer.println("# Sensitivity Analysis for Parameter: $paramName")
            writer.println("\nDate: ${LocalDate.now()}")

            writer.println("\n## Parameter Details")
            writer.println("* **Description**: ${describeParameter(paramName)}")
            writer.println("* **Original Value**: ${originalParameters[paramName]}")
            writer.println("* **Values Tested**: ${parameterValues[paramName]}")

            writer.println("\n## Results")

            // Create a comprehensive results table
            writer.println("| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |")
            writer.println("|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|")

            for (result in paramResults) {
                writer.println("| ${result.paramValue} | ${result.trapCount} | ${result.leakCount} | " +
                        "${result.migrationCount} | ${result.maturationsCount} | " +
                        "${String.format("%.1f", result.maxTemperature)} | ${String.format("%.1f", result.finalDepth)} | " +
                        "${result.simulationSteps} | ${String.format("%.1f", result.lastRockLayerSize)} | " +
                        "${result.typeErrors + result.warnings} | ${result.executionTime} | ${result.status} |")
            }

            writer.println("\n## Analysis")

            // Calculate and display parameter sensitivity
            if (paramResults.any { it.status == "SUCCESS" }) {
                val successfulResults = paramResults.filter { it.status == "SUCCESS" }

                // Find baseline result (result with original parameter value)
                val baselineResult = successfulResults.firstOrNull {
                    it.paramValue == originalParameters[paramName]
                } ?: successfulResults.first()

                writer.println("\n### Sensitivity Analysis")
                calculateSensitivity(writer, successfulResults, baselineResult)
            }
        }

        println("Parameter report generated at: ${reportFile.absolutePath}")
    }

    /**
     * Calculate sensitivity metrics for a parameter and write to the report
     */
    private fun calculateSensitivity(
        writer: java.io.PrintWriter,
        results: List<SimulationResult>,
        baseline: SimulationResult
    ) {
        // Calculate percentage change in various metrics
        writer.println("* **Effect on Traps**: ${calculateSensitivityString(results, baseline) { it.trapCount }}")
        writer.println("* **Effect on Leaks**: ${calculateSensitivityString(results, baseline) { it.leakCount }}")
        writer.println("* **Effect on Migrations**: ${calculateSensitivityString(results, baseline) { it.migrationCount }}")
        writer.println("* **Effect on Maturations**: ${calculateSensitivityString(results, baseline) { it.maturationsCount }}")
        writer.println("* **Effect on Maximum Temperature**: ${calculateSensitivityString(results, baseline) { it.maxTemperature.toInt() }}")
        writer.println("* **Effect on Final Depth**: ${calculateSensitivityString(results, baseline) { it.finalDepth.toInt() }}")
        writer.println("* **Effect on Simulation Steps**: ${calculateSensitivityString(results, baseline) { it.simulationSteps }}")
    }

    /**
     * Helper function to calculate sensitivity description for a parameter
     */
    private fun calculateSensitivityString(
        results: List<SimulationResult>,
        baseline: SimulationResult,
        metricSelector: (SimulationResult) -> Int
    ): String {
        val baselineValue = metricSelector(baseline)
        if (baselineValue == 0) return "No baseline data available"

        val changes = results.map {
            val metric = metricSelector(it)
            val percentChange = if (baselineValue != 0) {
                ((metric - baselineValue).toDouble() / baselineValue) * 100
            } else {
                if (metric == 0) 0.0 else 100.0
            }
            "${it.paramValue}: ${String.format("%.2f", percentChange)}%"
        }

        return changes.joinToString(", ")
    }

    /**
     * Helper function to calculate sensitivity value for a parameter
     */
    private fun calculateSensitivityValue(
        results: List<SimulationResult>,
        baseline: SimulationResult,
        metricSelector: (SimulationResult) -> Int
    ): Double {
        val baselineValue = metricSelector(baseline)
        if (baselineValue == 0) return 0.0

        return results.map {
            val metric = metricSelector(it)
            if (baselineValue != 0) {
                Math.abs((metric - baselineValue).toDouble() / baselineValue)
            } else {
                if (metric == 0) 0.0 else 1.0
            }
        }.average()
    }

    /**
     * Helper function to describe a parameter
     */
    private fun describeParameter(paramName: String): String {
        return when (paramName) {
            "SHALE_SIZE" -> "Thickness of shale source rock units"
            "CHALK_SIZE" -> "Thickness of chalk reservoir units"
            "SANDSTONE_SIZE" -> "Thickness of sandstone reservoir units"
            "BASE_TEMPERATURE" -> "Base temperature value for geothermal gradient calculation"
            "TEMP_FACTOR" -> "Temperature increase factor with depth"
            "HYDROCARBON_INCREMENT" -> "Rate of hydrocarbon generation and migration"
            "START_PAST" -> "Starting time in geological past (millions of years)"
            "CHECK_START" -> "Time to begin checking for maturation triggers"
            "DEPOSITION_DURATION" -> "Time span for each deposition event"
            "DIV_LAYERS" -> "Number of Div sandstone layers"
            "TOR_LAYERS" -> "Number of Tor chalk layers"
            "EKOFISK_LAYERS" -> "Number of Ekofisk chalk layers"
            "CAP_LAYERS" -> "Number of cap rock (seal) layers"
            "AB1_LAYERS" -> "Number of AB1 sandstone layers"
            "AB2_LAYERS" -> "Number of AB2 sandstone layers"
            else -> "Unknown parameter"
        }
    }

    /**
     * Helper function to describe the sensitivity of a parameter
     */
    private fun describeSensitivity(paramName: String): String {
        return when (paramName) {
            "SHALE_SIZE" -> "Changes in shale layer thickness significantly affect hydrocarbon maturation and migration patterns"
            "CHALK_SIZE", "SANDSTONE_SIZE" -> "Reservoir rock thickness affects storage capacity and hydrocarbon accumulation"
            "BASE_TEMPERATURE", "TEMP_FACTOR" -> "Temperature parameters are critical for kerogen maturation rates"
            "HYDROCARBON_INCREMENT" -> "Rate of hydrocarbon generation affects overall system dynamics"
            "START_PAST" -> "Starting time in the geological past affects the total simulation duration"
            "CHECK_START" -> "Timing of maturation onset is crucial for hydrocarbon generation"
            "DEPOSITION_DURATION" -> "Speed of deposition affects layer formation and timing"
            "DIV_LAYERS", "TOR_LAYERS", "EKOFISK_LAYERS", "CAP_LAYERS", "AB1_LAYERS", "AB2_LAYERS" ->
                "Number of deposited layers affects total rock column and pressure conditions"
            else -> "Unknown parameter impact"
        }
    }

    /**
     * Helper function to get the primary impact of a parameter
     */
    private fun getParameterPrimaryImpact(paramName: String): String {
        return when (paramName) {
            "SHALE_SIZE" -> "hydrocarbon generation potential and source rock capacity"
            "CHALK_SIZE", "SANDSTONE_SIZE" -> "reservoir capacity and hydrocarbon storage potential"
            "BASE_TEMPERATURE", "TEMP_FACTOR" -> "kerogen maturation rates and timing of hydrocarbon generation"
            "HYDROCARBON_INCREMENT" -> "migration efficiency and trapping dynamics"
            "START_PAST", "CHECK_START" -> "basin evolution timeline and petroleum system timing"
            "DEPOSITION_DURATION" -> "sedimentation rates and sequence development"
            "DIV_LAYERS", "TOR_LAYERS", "EKOFISK_LAYERS", "CAP_LAYERS", "AB1_LAYERS", "AB2_LAYERS" ->
                "basin architecture and stratigraphic complexity"
            else -> "multiple aspects of the petroleum system"
        }
    }

    /**
     * Data class to represent simulation output metrics
     */
    data class OutputMetrics(
        val trapCount: Int,
        val leakCount: Int,
        val migrationCount: Int,
        val maturationsCount: Int,
        val maxTemperature: Double,
        val finalTime: Double,
        val finalDepth: Double,
        val buildSuccess: Boolean,
        val simulationSteps: Int,
        val lastRockLayerSize: Double,
        val typeErrors: Int,
        val warnings: Int,
        val executionSuccess: Boolean
    )

    /**
     * Data class to represent a simulation result
     */
    data class SimulationResult(
        val paramName: String,
        val paramValue: Double,
        val trapCount: Int,
        val leakCount: Int,
        val migrationCount: Int,
        val maturationsCount: Int,
        val maxTemperature: Double,
        val executionTime: Long,
        val status: String,
        val finalTime: Double = 0.0,
        val finalDepth: Double = 0.0,
        val simulationSteps: Int = 0,
        val lastRockLayerSize: Double = 0.0,
        val typeErrors: Int = 0,
        val warnings: Int = 0,
        val executionSuccess: Boolean = false
    )
}

/**
 * Capture the default output from running the simulation
 */
fun captureDefaultOutput(scriptPath: String, outputPath: String) {
    val outputFile = File(outputPath)

    println("Running default simulation to capture baseline output...")

    val process = ProcessBuilder("bash", scriptPath)
        .redirectOutput(outputFile)
        .start()

    val completed = process.waitFor(10, TimeUnit.MINUTES)

    if (completed) {
        println("Default simulation completed. Output saved to: ${outputFile.absolutePath}")
    } else {
        println("Default simulation timed out after 10 minutes")
        process.destroy()
    }
}

/**
 * Main function to run the sensitivity analysis
 */
fun main(args: Array<String>) {
    val projectRoot = if (args.isNotEmpty()) args[0] else System.getProperty("user.dir")
    val smolFilePath = if (args.size > 1) args[1] else "$projectRoot/examples/Geological/simulate_onto.smol"
    val outputDir = if (args.size > 2) args[2] else "$projectRoot/sensitivity_analysis_results"

    println("Starting SMOL Sensitivity Analysis")
    println("Project Root: $projectRoot")
    println("SMOL File: $smolFilePath")
    println("Output Directory: $outputDir")

    // Capture default output first
    val executeScript = "$projectRoot/execute_case.sh"
    File(outputDir).mkdirs()
    captureDefaultOutput(executeScript, "$outputDir/default_simulation_output.txt")

    // Create the sensitivity analysis object
    val analysis = SmolSensitivityAnalysis(
        projectRoot = projectRoot,
        smolFilePath = smolFilePath,
        outputDir = outputDir
    )

    if (args.size > 3) {
        // Run analysis for a specific parameter only
        val paramName = args[3]
        println("Running sensitivity analysis for parameter: $paramName")
        analysis.runParameterAnalysis(paramName)
    } else {
        // Run full analysis
        println("Running full sensitivity analysis...")
        val startTime = System.currentTimeMillis()
        analysis.runSequentialSensitivityAnalysis()
        val duration = System.currentTimeMillis() - startTime
        println("Sensitivity analysis completed in ${duration / 1000.0} seconds")
    }
}