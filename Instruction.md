# 2. Set Up Docker Environment

## 1) Modify the existing `Dockerfile` file to the following code.

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

## 2) Navigate to the root directory of the cloned repository.

Use the `cd` command to navigate to the root directory of SemanticObject on the local drive. After that, switch to the `geosim` branch.

```bash
git checkout geosim
```

## 3) Build the Docker image.

```bash
docker build -t smolang .
```

## 4) Run the simulation case study.

```bash
docker run -v $(pwd):/app smolang ./execute_case.sh
```

To capture both standard output and error output, use the following command:

```bash
docker run -v $(pwd):/root/smol smolang ./execute_case.sh &> output.txt
```

Running the simulation with this command will execute it in the background, redirecting the output to the `output.txt` file.

## 1) Main Entry Point: `MainKt.kt`

File path:

```bash
./src/main/kotlin/no/uio/microobject/main/MainKt.kt
```

`MainKt.kt` serves as the main entry point for the semantic object simulation program described in Qu et al.'s paper. It handles program initialization, command-line argument processing, and provides both interactive and automated execution modes.

Note that the core functionality of the program is executed through an instance of the `REPL` class, defined in `REPL.kt`.

### How to Interact with the Program

#### 1. Command Line Options

- `--execute (-e)`: Run in automatic execution mode.
- `--load (-l)`: Run in interactive REPL mode (default).
- `--back (-b)`: Specify path to OWL background knowledge file.
- `--domain (-d)`: Set domain prefix for ontology.
- `--input (-i)`: Specify input `.smol` file to load at startup.
- `--replay (-r)`: Provide a file containing REPL commands to execute.
- `--outdir (-o)`: Set output directory for generated files.
- `--verbose (-v)`: Enable detailed output.
- `--materialize (-m)`: Generate and save triples to file.
- `--useQueryType (-q)`: Enable type checking for access.
- `--prefixes (-p)`: Define additional prefixes in `PREFIX=URI` format.

#### 2. Interactive REPL Mode

- Start the program without `--execute` flag.
- Use the `MO>` prompt to enter commands.
- Commands are structured as: `command argument1 argument2...`
- Type `help` to see available commands.
- Use `Ctrl+D` to exit.

#### 3. Automatic Mode

- Start with `--execute` flag.
- Provide input file with `--input`.
- Program runs to completion automatically.

## 2) REPL.kt



## 3) `OwlStmt.kt`

`OwlStmt.kt` defines a statement type in the SMOL language that enables the simulation program to query the ontology and use the results to drive the simulation, realising the connection between the ontology and the simulator that is central to the method proposed in the paper.

### Relationship between `OwlStmt.kt` and Enhanced Lifting

**Enhanced Lifting** is the process of connecting the RDF graph generated from the program state (through direct lifting) with the domain ontology.

In the proposed method, **enhanced lifting** is facilitated by the **modeling bridge** or **bridgehead object**, which enables the connection between the program state and a corresponding individual in the domain ontology. This connection is established through an annotation in the `SMOL` program, which is an annotation with *a models keyword followed by a string containing a predicate-object list*.

For example:

```kotlin
class ShaleUnit models "a Sedimentary_Geological_Object ; located_in some Organic_Matter ; composed_of some Shale ; has_temperature %temperature"
```

`OwlStmt.kt` represents a statement in the `SMOL` language that queries the ontology and binds the results to a program variable. It is a PART of the computational modeling and simulation aspect of the method.

However, `OwlStmt.kt` does interact with the results of the enhanced lifting. When the OWL query is executed, it operates on the ontology that has been populated with information from the program state via the lifting process. The `eval()` function then translates the query results (OWL named individuals) back into the program's memory model, constructing a linked list in the heap.

## 4) `SimulationStmt.kt`

`SimulationStmt.kt` represents a simulation statement in the `SMOL` language used to create and assign a simulation object (an instance of the `SimulatorObject` class) to a target variable.

The `SimulationStmt.kt` class is part of the computational modelling aspect of the proposed method, which allows geologists to *create and manipulate simulation objects* within the `SMOL` language, which can then be used to simulate geological processes.

The `SimulationStmt` class enables the integration of external simulation models (FMUs) into the `SMOL` language. This integration allows the program to bridge the gap between the *static geological knowledge* modelled in ontologies and the *dynamic behaviour of geological processes* simulated using computational methods.