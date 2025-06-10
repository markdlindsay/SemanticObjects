# Geological Model Sensitivity Analysis Report

Date: 2025-03-07

## Baseline Simulation Results

The default simulation with original parameter values produced:
* **Hydrocarbon Traps**: 0
* **Hydrocarbon Leaks**: 0
* **Shale Migrations**: 0
* **Kerogen Maturations**: 0
* **Maximum Temperature**: 0.00°C
* **Final Depth**: 0.00 units
* **Simulation Steps**: 0
* **Final Time**: 0.0
* **Final Rock Layer Size**: 0.0
* **Build Errors**: 0
* **Build Warnings**: 0

## Sensitivity Analysis Results

### Parameter: SHALE_SIZE

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Thickness of shale source rock units
* **Range Tested**: [20.0, 30.0, 40.0, 50.0, 60.0]
* **Original Value**: 40.0


### Parameter: CHALK_SIZE

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Thickness of chalk reservoir units
* **Range Tested**: [50.0, 75.0, 99.0, 125.0, 150.0]
* **Original Value**: 99.0


### Parameter: SANDSTONE_SIZE

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Thickness of sandstone reservoir units
* **Range Tested**: [13.0, 19.5, 26.5, 33.0, 40.0]
* **Original Value**: 26.5


### Parameter: BASE_TEMPERATURE

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Base temperature value for geothermal gradient calculation
* **Range Tested**: [1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0]
* **Original Value**: 2.5


### Parameter: TEMP_FACTOR

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Temperature increase factor with depth
* **Range Tested**: [15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0]
* **Original Value**: 30.0


### Parameter: HYDROCARBON_INCREMENT

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Rate of hydrocarbon generation and migration
* **Range Tested**: [50.0, 75.0, 100.0, 125.0, 150.0]
* **Original Value**: 100.0


### Parameter: START_PAST

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Starting time in geological past (millions of years)
* **Range Tested**: [100.0, 115.0, 130.0, 136.0, 145.0, 160.0]
* **Original Value**: 136.0


### Parameter: CHECK_START

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Time to begin checking for maturation triggers
* **Range Tested**: [-80.0, -75.0, -70.0, -66.0, -60.0, -55.0, -50.0]
* **Original Value**: -66.0


### Parameter: DEPOSITION_DURATION

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Time span for each deposition event
* **Range Tested**: [1.0, 1.5, 2.0, 2.5, 3.0]
* **Original Value**: 2.0


### Parameter: DIV_LAYERS

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Number of Div sandstone layers
* **Range Tested**: [20.0, 25.0, 31.0, 35.0, 40.0]
* **Original Value**: 31


### Parameter: TOR_LAYERS

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Number of Tor chalk layers
* **Range Tested**: [3.0, 4.0, 5.0, 6.0, 7.0]
* **Original Value**: 5


### Parameter: EKOFISK_LAYERS

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Number of Ekofisk chalk layers
* **Range Tested**: [1.0, 2.0, 3.0]
* **Original Value**: 1


### Parameter: CAP_LAYERS

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Number of cap rock (seal) layers
* **Range Tested**: [1.0, 2.0, 3.0]
* **Original Value**: 1


### Parameter: AB1_LAYERS

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Number of AB1 sandstone layers
* **Range Tested**: [3.0, 4.0, 5.0, 6.0, 7.0]
* **Original Value**: 5


### Parameter: AB2_LAYERS

| Value | Traps | Leaks | Migrations | Maturations | Max Temp | Final Depth | Sim Steps | Rock Size | Build Issues | Exec Time (ms) | Status |
|-------|-------|-------|------------|-------------|----------|-------------|-----------|-----------|--------------|----------------|--------|

#### Analysis

* **Parameter**: Number of AB2 sandstone layers
* **Range Tested**: [20.0, 23.0, 26.0, 29.0, 32.0]
* **Original Value**: 26


## Visualization Recommendations

For better visualization of the sensitivity analysis results, we recommend creating the following charts:
1. **Parameter vs. Trap Count**: Bar charts showing how trap count varies with parameter values
2. **Parameter vs. Max Temperature**: Line charts showing temperature response curves
3. **Parameter vs. Migration Events**: Trend analysis of hydrocarbon migration patterns
4. **3D Surface Plot**: Showing parameter interactions (e.g., temperature factor vs. shale size vs. trap count)

These visualizations can be generated from the tabular data above using tools like Excel, Python (matplotlib/seaborn), or R.

## Overall Conclusions

Based on the sensitivity analysis, the parameters with the most significant impact on the model outcomes are:

| Rank | Parameter | Sensitivity Score | Primary Impact |
|------|-----------|-------------------|----------------|

### Key Takeaways

