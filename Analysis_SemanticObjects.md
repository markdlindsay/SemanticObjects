# 1. Understanding the Program

## 1) Geology Knowledge Modeling Segment

Defined in ontology files:
- `/examples/Geological/microex.ttl`: Contains basic geological entity definitions (chalk, shale, fault, etc.)
- `/examples/Geological/total_mini.ttl`: Contains the broader ontology framework using BFO (Basic Formal Ontology).
- `/src/main/resources/vocabulary.owl`: Provides core semantic vocabulary

### i. Process Triggers for Geological Events

From `total_mini.ttl` and examples in Qu's paper, the core trigger structure is defined as:

```bash
untitled-ontology-38:trigger
        rdf:type owl:Class ;
        rdfs:subClassOf obo:BFO_0000035 .
```

Triggers are modeled as:
- Process boundaries in BFO framework
- Instantaneous temporal points when conditions are met
- Connected to specific geological processes through the `trigger_by` property

Here are a full list of triggers in the code:
1. `thermal_maturation_trigger`: Activates when temperature reaches oil/gas window.
2. `oil_window_maturation_trigger`: Specific temperature range 60-120°C
3. `gas_window_maturation_trigger`: Temperature range 100-200°C
4. `fracturing_trigger`: Based on tensile strength thresholds
   1) `chalk_unit_fracturing_trigger`
   2) `shale_fracturing_trigger`
5. `fluid_migration_trigger`: Activates when geological object contains earth fluid, connected to permeable rock, and externally connected with permeable rock

### ii. Material Entities

From `microex.ttl`, we observe a material hierarchy:

1. Constituent (base class)
   - Chalk
   - Shale
2. Object (base class)
   - Unit
   - Fault
3. Independent Classes
   - Oil
   - Rock

### iii. Quality Measurements

Measurements are defined under BFO framework. They are subclasses of `BFO:quality`. Each measurement has `datavalue` property to store numeric values.

List of Measurements:
1. Physical Properties:
   - Temperature (°C): Used in maturation triggers
     - Key thresholds: 60-120°C for oil window, 100-200°C for gas window 
   - Tensile strength (MPa): Controls fracturing processes 
     - Thresholds:
       - ≥ 0.36 MPa for chalk fracturing
       - ≥ 0.5 MPa for shale fracturing
2. Reservoir Properties:
   - Permeability: Binary quality (permeable/impermeable), which controls fluid migration
3. Spatial Properties:
   - Depth 
   - Thickness 
   - Size (in meters)

All qualities use the same structure:
- A quality class definition
- A datavalue property for numeric values 
- Units specified in ontology 
- Valid ranges for different processes

### iv. Geological Relationships

1. Spatial relationships:

```turtle
:above rdf:type owl:ObjectProperty
:below rdf:type owl:ObjectProperty
:front rdf:type owl:ObjectProperty
:behind rdf:type owl:ObjectProperty
:left rdf:type owl:ObjectProperty
:right rdf:type owl:ObjectProperty
:externally_connected_with rdf:type owl:ObjectProperty
```

2. Material composition relationships:

```turtle
:constituted_by rdf:type owl:ObjectProperty
- Links geological objects to their constituent materials
- Examples:
  - Ekofisk Formation constituted_by chalk
  - Mandal Formation constituted_by shale
```

3. Content relationships:

```turtle
:location_of rdf:type owl:ObjectProperty
:amount_of_organic_matter rdf:type owl:Class
- Used for source rocks containing organic matter
- Links to kerogen, oil, gas content
```

4. Process relationships:

```turtle
:generated_by rdf:type owl:ObjectProperty
    rdfs:subPropertyOf obo:RO_0000056 # participates_in
```

5. Functional roles:

```turtle
:source_rock rdf:type owl:Class
    rdfs:subClassOf obo:BFO_0000023 #role
:reservoir rdf:type owl:Class 
    rdfs:subClassOf obo:BFO_0000023
:cap_rock/seal rdf:type owl:Class
    rdfs:subClassOf obo:BFO_0000023
:migration_path rdf:type owl:Class
    rdfs:subClassOf obo:BFO_0000023
```

6. System relationship:

```turtle
:petroleum_system rdf:type owl:Class
    rdfs:subClassOf obo:BFO_0000027 #object_aggregate
```

The 6 categories from the ontology files can be condensed into 4 broader patterns:

1. Stratigraphic relationship describe how rock layers are stacked vertically.

   - For example, in Ekofisk:
     - Vaale Formation (top)
     - Ekofisk Formation (middle)
     - Mandal Formation (bottom)

2. Containment relationship describe what materials are held within rock formations. 
   - Source rocks (like Mandal Formation) contain organic matter (kerogen). When heated, kerogen transforms into oil/gas.
   - Reservoir rocks (like Ekofisk Formation) hold the oil/gas that migrated from source rocks. They must be porous enough to store hydrocarbons. 
   - Seal rocks (like Vaale Formation) block hydrocarbons from escaping. They must be impermeable.

3. Process chain relationships describe the sequence of petroleum system events. 
   - Step 1: Generation
     - Source rock heats up
     - Kerogen converts to oil/gas
   - Step 2: Migration
     - Oil/gas moves upward through permeable rocks
     - Following paths of least resistance 
   - Step 3: Trapping
     - Hydrocarbons collect in reservoir rocks 
     - Stopped by impermeable seal rocks above

4. System component relationships describe how all parts work together.
   - The essential trio:
     - Source rock (generates hydrocarbons)
     - Reservoir rock (stores hydrocarbons)
     - Seal rock (traps hydrocarbons)
   - Connected by:
     - Migration pathways through permeable rocks
     - Timing must be right: seal must be in place before migration occurs

## 2) Bridge/Connection Segment

### i. `/src/main/kotlin/no/uio/microobject/data/TripleManager.kt`

`TripleManager.kt` is the central hub for connecting knowledge and computation.

The main bridging functions are:
- `getModel()`: Provides RDF model combining all data sources .
- `getOntology()`: Provides OWL ontology for reasoning.
- `getStaticDataOntology()`: Provides ontology excluding runtime data.

Manages three types of data:

- Heap triples (runtime data)
- Static table triples (program structure)
- External ontology triples (geological knowledge)

Provides virtual triple patterns to connect program state with ontology concepts.

### ii. `/src/main/kotlin/no/uio/microobject/ast/Translate.kt`

`Translate.kt` converts between ANTLR syntax and internal representations (knowledge to code). Its key bridging mechanisms are:
- Translates domain annotations to code structures.
- Maps ontological concepts to program entities.
- Maintains semantic links during compilation.


### iii. `/src/main/kotlin/no/uio/microobject/runtime/State.kt`

`State.kt` defines bridging data structures.

```kotlin
// Links domain concepts to code
data class FieldInfo(
    val name: String,
    val type: Type,
    val computationVisibility: Visibility,
    val declaredIn: Type,
    val isDomain: Boolean    // Flag for domain-linked fields
)

// Connects methods to domain rules
data class MethodInfo(
    val stmt: Statement,
    val params: List<String>,
    val isRule: Boolean,     // Flag for domain rules
    val isDomain: Boolean,   // Flag for domain methods
    val declaringClass: String,
    val retType: Type
)
```

### iv. How the Bridging Works

1. Loading Phase

```kotlin
// 1. Load ontologies
tripleManager.getModel()  // Loads all RDF data

// 2. Parse program code with domain annotations
visitor.generateStatic(tree)  // Creates annotated AST

// 3. Generate static structures
val staticTable = StaticTable(
    fieldTable,    // Domain-linked fields
    methodTable,   // Domain-linked methods
    hierarchy,     // Class relationships
    modelsTable,   // Domain models
    hiddenSet      // Hidden elements
)
```

2. Runtime Connection

```kotlin
// 1. Check domain rules through queries
val results = interpreter.query(str)

// 2. Validate against geological constraints
val reasoner = Reasoner(ontology)
reasoner.isConsistent()

// 3. Process triggers
"SELECT ?obj WHERE { ... }" // Query for triggered processes
```

3. State Synchronization

```kotlin
// Computational state -> Semantic state
heap.keys.forEach { obj ->
    // Generate RDF triples representing state
    Triple(obj, predicate, value)
}

// Semantic state -> Computational state
interpreter.owlQuery(query).forEach { result ->
    // Update program state based on query results
}
```

The bridging layer enables:

1. Semantic Validation:
   - Checks geological consistency of operations
   - Validates state changes against domain rules
   - Ensures semantic correctness of simulation
2. Process Triggering:
   - Monitors conditions in ontology
   - Activates computational processes when triggered
   - Maintains geological timeline consistency
3. Knowledge Integration:
   - Maps domain concepts to program structures 
   - Maintains bidirectional state synchronization 
   - Enables reasoning over program state

## 3) Computational Modeling Segment

The computational layer serves as the program's active execution engine. At its core, it executes the basic program logic through the interpreter, which processes statements and evaluates expressions based on standard programming semantics. While doing this, it maintains the program state across multiple memory spaces: local variables in stack frames, object data in the heap, and simulation states in dedicated memory. The layer interfaces with simulations by managing time progression, state transitions, and history tracking through the SimulatorObject class. When conditions specified in the geological ontology are met, the layer responds to semantic triggers by executing corresponding processes, such as oil maturation or rock fracturing. Throughout execution, it continuously updates the knowledge base by generating RDF triples that reflect the current state, ensuring the semantic model remains synchronised with the computational results. This bidirectional flow between computation and knowledge representation ensures the simulation remains geologically valid while advancing through time.

Core execution implemented in:
- `/src/main/kotlin/no/uio/microobject/runtime/Interpreter.kt`: main execution engine
- `/src/main/kotlin/no/uio/microobject/runtime/REPL.kt`: Interactive environment
- `/src/main/kotlin/no/uio/microobject/runtime/Simulation.kt`: Simulation state management

### i. Interpreter (`Interpreter.kt`)

```kotlin
class Interpreter(
    val stack: Stack<StackEntry>,        // Execution stack
    var heap: GlobalMemory,              // Object storage
    var simMemory: SimulationMemory,     // Simulation state
    val staticInfo: StaticTable,         // Program structure
    val settings: Settings               // Configuration
) {
    // Core execution method
    fun makeStep(): Boolean {
        if(stack.isEmpty()) return false   // Program terminated
        val current = stack.pop()          // Get current frame
        val heapObj = heap[current.obj]    // Get object memory
        val res = current.active.eval()    // Execute statement
    }
}
```

### ii. REPL Environment (`REPL.kt`)

```kotlin
class REPL(private val settings: Settings) {
    private var interpreter: Interpreter? = null
    
    // Command processing
    fun command(str: String, param: String): Boolean {
        // Execute commands like:
        // - read (load files)
        // - step (execute one step)
        // - auto (run to completion)
        // - query (execute semantic queries)
    }
}
```

### iii. Simulation Management (`Simulation.kt`)

```kotlin
class SimulatorObject(val path: String, memory: Memory) {
    private var sim: Simulation          // Simulation state
    private var time: Double            // Current time
    private val series = mutableList<Snapshot>()  // History
    
    fun tick(i: Double) {               // Advance simulation
        sim.doStep(i)
        time += i
        addSnapshot()
    }
}
```

## 4) The Workflow of the Program

### i. Loading

Geological ontologies are loaded through `TripleManager`.

Program code is parsed through ANTLR and `Translate`.

Static structures are generated linking code to ontology concepts.

### ii. Execution

`Interpreter` executes program logic

When encountering semantic triggers (defined in ontology):

- Queries ontology through `TripleManager`
- Validates against geological rules 
- Updates program state accordingly

### iii. Bridging during Execution

Process triggers are checked through ontology queries.

Object states are validated against geological constraints.

Results from computation are reflected back into semantic model.

# 2. Major Variables

## 1) Geology Knowledge Modeling Variables

```kotlin
data class StaticTable(
    val fieldTable: Map<String, FieldEntry>,
    val methodTable: Map<String, Map<String, MethodInfo>>,
    val hierarchy: MutableMap<String, MutableSet<String>>,
    val modelsTable: Map<String, List<ModelsEntry>>,
)
```

- `fieldTable`: Maps class names to their fields, which is ritical for representing geological structures (e.g., layers, formations). It stores properties like temperature, permeability, composition and is essential for process simulation as these properties determine behaviour.
- `methodTable`: Maps class names to method definitions and implements geological process behaviours. It handles reactions to condition changes and contains rules for process triggers.
- `hierarchy`: Represents geological classification relationships and maps superclass to set of subclasses. It is important for inheritance of properties and behaviours and used in semantic reasoning about geological relationships.
- `modelsTable`: Maps classes to semantic model descriptions and contains ontological annotations, which is critical for connecting domain knowledge to computation and enables semantic querying of geological structures.

## 2) Computational Modeling Variables

```kotlin
class SimulatorObject(
    val path: String,
    val series: MutableList<Snapshot>,
    private var time: Double,
    private var role: String,
)
```
- `path`: Location of simulation configuration, which points to model description files and is crucial for initialising simulations.
- `series`: Records simulation history and stores state changes over time, which is important for analysis and validation and used in result visualisation
- `time`: Current simulation time point, which tracks progression of geological processes and is critical for synchronising multiple processes.
- `role`: Function in the simulation, which is used in semantic annotations to identify purpose in larger system.

## 3) Bridging/Semantic Lifting Variables

```kotlin
class TripleManager(
    val currentTripleSettings: TripleSettings(
        sources: HashMap<String,Boolean>,
        guards: HashMap<String,Boolean>,
        virtualization: HashMap<String,Boolean>,
    )
)
```
- `sources`: Controls knowledge source inclusion, which is critical for managing knowledge integration
  - "heap": Runtime object data
  - "staticTable": Static program structure
  - "vocabularyFile": Core ontology definitions
  - "externalOntology": Domain knowledge
- `guards`: Controls semantic query optimisation and improves query performance. It also maintains knowledge consistency and is essential for large-scale simulations.
- `virtualization`: Controls triple generation approach and affects memory usage and performance. It is important for handling large datasets.

# 3. How to Run the Program

## 1) Remove Lines from `build.gradle`

As `SemanticObjects` contributor `Edkamb` commented, the following lines are related to an experimental feature they tested, which is not essential. They also remove these two lines from their branches.

For details, check out this link: https://github.com/smolang/SemanticObjects/issues/50

```bash
maven { url "https://overture.au.dk/artifactory/libs-release/" }
```

```bash
implementation "into-cps:scenario_verifier_2.13:0.2.14"
```

## 2) Set Up Docker Environment

### i. Modify the existing `Dockerfile` file to the following code.

```dockerfile
# syntax=docker/dockerfile:1.3-labs

# To build the container:
#     docker build -t smol .
# To run smol in the current directory:
#     docker run -it --rm -v "$PWD":/root/smol smol
FROM ubuntu:latest
RUN <<EOF
    apt-get -y update
    DEBIAN_FRONTEND=noninteractive apt-get -y install openjdk-11-jdk-headless python3 python3-pip python3-venv liblapack3
    rm -rf /var/lib/apt/lists/*
    python3 -m venv /opt/venv
    . /opt/venv/bin/activate
    pip3 install pyzmq
EOF
COPY . /usr/local/src/smol
WORKDIR /usr/local/src/smol
RUN ./gradlew --no-daemon assemble
WORKDIR /root/smol
CMD ["java", "-jar", "/usr/local/src/smol/build/libs/smol-0.2-all.jar"]
# CMD java -jar /usr/local/src/smol/build/libs/smol-0.2-all.jar -i examples/House/osphouseV2.smol -e -b examples/House/rooms.owl -p asset=https://github.com/Edkamb/SemanticObjects/Asset# -m
```

### ii. Navigate to the root directory of the cloned repository.

Use the `cd` command to navigate to the root directory of SemanticObject on the local drive. After that, switch to the `geosim` branch.

```bash
git checkout geosim
```

### iii. Build the Docker image.

```bash
docker build -t smolang .
```

### iv. Run the simulation case study.

```bash
docker run -v $(pwd):/app -w /app smolang ./execute_case.sh
```

To capture both standard output and error output, use the following command:

```bash
docker run -v $(pwd):/root/smol smolang ./execute_case.sh &> output.txt
```

Running the simulation with this command will execute it in the background, redirecting the output to the `output.txt` file.

## 3) Scenario Files Used by the Program

### i. `execute_case.sh` Launches the Program

```bash
#!/bin/bash
./gradlew shadowJar
java -jar build/libs/smol.jar -i examples/Geological/simulate_onto.smol -v -e -b examples/Geological/total_mini.ttl -p UFRGS1=https://www.inf.ufrgs.br/bdi/ontologies/geocoreontology#UFRGS -p obo=http://purl.obolibrary.org/obo/ -d http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#
```

Inside `execute_case.sh`, we can define input scenario file (`.smol`) path and the background ontology file (`.ttl`) path. 

### ii. Simulation Model

`simulate_onto.smol` defines the core geological simulation code, which contains:

#### 1. Main Program Setup
1. Initial Shale Layer (Mandal Formation)

```smol
ShaleUnit mandal = new ShaleUnit(null, null, null, null, null, null, 40.0, 1, 0.0, True, 0);
```

- Thickness: 40.0m
- mergeId: 1
- Initial temperature: 0.0
- Has kerogen source: True
- Initial matured units: 0

2. Deposition Sequence (from bottom to top)
- Deep marine deposits
- Tor Formation (chalk)
- Ekofisk Formation (chalk)
- Cap layer (mudstone modelled as shale)
- More deep marine deposits
- Shallow marine deposits

3. Simulation Parameters
- Time span: 136.0 million years ago to start
- Initial unit: mandal formation
- Trigger check start time: -66.0 million years ago

Each `DepositionGenerator` is configured with:
- Unit template (thickness and properties)
- Time step (2.0 million years)
- Number of repetitions

The `mergeId` system (1 or 2) determines which units can merge:
- `mergeId 1`: Shale/mudstone units
- `mergeId 2`: Sandstone/chalk units

#### 2. Abstract Base Class: `GeoObject`

- Fields:
  - `above`, `below`, `left`, `right`, `behind`, `front`: Hidden `GeoObject` references for spatial relationships
  - `size`: Hidden `Double` for the thickness of the unit
- Methods:
  - `getSizeAbove()`: Calculates total thickness above this unit
  - `update()`: Abstract method for unit updates
  - `updateAll()`: Updates this unit and all units above it 
  - `caps()`: Abstract method for containment capability 
  - `addUnit()`: Abstract method for unit addition 
  - `printState()`: Abstract method for state output

#### 3. `Fault` Class (extends `GeoObject`)

- Basic implementation with no additional fields
- All methods are empty/skip implementations

#### 4. Abstract Class: `GeoUnit` (extends `GeoObject`)

- Additional Fields:
  - `mergeId`: Hidden `Integer` for identifying mergeable units
- Methods:
  - `clone()`: Abstract method for unit duplication 
  - `canMerge()`: Checks if units can be merged based on `mergeId`
  - `mergeWith()`: Merges units by combining their sizes

#### 5. `ChalkUnit` (extends `GeoUnit`)

- Fields:
  - `kerogenUnits`: List of `Double` values tracking kerogen positions
- Semantics: Models as a geological object constituted by chalk
- Methods:
  - `update()`: Handles kerogen migration upwards 
  - `caps()`: Returns false (does not trap kerogen)
  - `addUnit()`: Adds new kerogen unit

#### 6. `SandstoneUnit` (extends `GeoUnit`)

- Fields:
  - `kerogenUnits`: List of `Double` values for kerogen tracking
- Semantics: Models as a geological object constituted by sandstone

#### 7. `ShaleUnit` (extends `GeoUnit`)

- Fields:
  - `temperature`: Hidden `Double` for unit temperature 
  - `hasKerogenSource`: Hidden `Boolean` indicating kerogen presence 
  - `maturedUnits`: Hidden `Integer` counting matured kerogen units
- Semantics: Models as a sedimentary geological object with organic matter
- Methods:
  - `update()`: Updates temperature and handles matured unit migration
  - `mature()`: Handles maturation process 
  - `caps()`: Returns true (can trap kerogen)

#### 8. Hidden Class: `DepositionGenerator`

- Fields:
  - `emitUnit`: `GeoUnit` template for emission
  - `duration`: `Double` for time steps
  - `times`: `Integer` for repetition count
- Methods:
  - `emit()`: Creates new unit instances

#### 9. Hidden Class: Driver

The `Driver` class is responsible for:
1. Managing simulation state and time progress
2. Running the simulation through its main `sim` method
3. Handling unit deposition
4. Triggering geological processes

- Fields:
  - `top`, `bottom`: `GeoUnit` references
- Methods:
  - `sim()`: Main simulation control
- Parameters:
  - `actions`: List of `DepositionGenerator`
  - `startPast`: Initial time
  - `init`: Initial unit
  - `checkStart`: Time to begin checks

### iii. Ontology

`total_mini.ttl` contains an ontology that defines the core concepts and relationships for the geological simulation.

1. Basic Formal Ontology (BFO) Framework
2. Geological Concepts
   - Defines geological material types (rock, fluid, etc.)
   - Specifies geological objects and their properties
   - Models geological processes and their boundaries
   - Establishes relationships between geological entities
3. Property Definitions
   - Spatial relationships (above, below, adjacent)
   - Material properties (temperature, composition)
   - Temporal relationships (sequence of events)
   - Process-related properties (triggers, effects)
4. Domain-Specific Elements
   - Geological age and time intervals
   - Rock types and their characteristics
   - Earth materials and their classifications
   - Geological boundaries and contacts

