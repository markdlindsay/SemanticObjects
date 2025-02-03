@file:Suppress("ControlFlowWithEmptyBody")

package no.uio.microobject.runtime

import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Translate
import no.uio.microobject.data.TripleManager
import no.uio.microobject.main.Settings
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.apache.jena.query.ResultSetFormatter
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.File
import java.time.Duration
import java.time.LocalTime
import java.util.*

/*
The class provides a clean abstraction for defining and executing REPL commands with consistent parameter handling and help text.
1. Commands are created in REPL.initCommands() to define all available REPL functionality.
2. The command function takes a String parameter and returns a Boolean that indicates whether the REPL should exit - used for the "exit" command to return true
3. requiresFile determines if a .smol file needs to be loaded first. For example:
    - "read" command has requiresFile=false since it loads a file
    - "step" command has requiresFile=true since it needs code to execute
4. The execute() method provides parameter validation before running the command function.
 */
class Command(
    val name: String,                       // Name of the command
    private val repl: REPL,                 // Reference to the REPL instance that owns this command
    /*
    Takes a String as input parameter. Returns a Boolean as output.
    It defines a property that holds a function rather than a simple value.
     */
    val command: (String) -> Boolean,
    val help: String,                       // Help text describing what the command does
    val requiresParameter: Boolean = false, // Default value sets to false; if true, command needs a parameter string
    val parameterHelp: String = "",         // Description of expected parameter format
    val requiresFile: Boolean = true        // Default value sets to true; if true, a file must be loaded before command can run
) {
    fun execute(param: String): Boolean {
        // Check if parameter is required but missing
        if (requiresParameter && param == "") {
            repl.printRepl("Command $name expects 1 parameter $parameterHelp.")
            return false
        }
        // Run the command function with the parameter
        return command(param)
    }
}

// This annotation tells the Kotlin compiler to suppress warnings about deprecated code usage. It is used because ReasonerFactory from HermiT is deprecated, but they keep using it to make future changes easier.
@Suppress("DEPRECATION")
/*
The Settings object is defined in MainKt.kt, which holds configuration for the REPL.
*/
class REPL(private val settings: Settings) {
    /*
    The ? makes interpreter a nullable type, meaning it can hold either:
    - An Interpreter object, which is an instance of the Interpreter class. The Interpreter class is defined in Interpreter.kt.
    - null (This is Kotlin's way of handling null safety. Without ?, the variable could not be null. With ?, we must handle potential null cases when using the interpreter.)
     */
    private var interpreter: Interpreter? = null
    private val commands: MutableMap<String, Command> = mutableMapOf()
    /*
    The init block is a special block in Kotlin that runs when an object is constructed, immediately after the constructor. It is like a constructor body that runs for every constructor. You can have multiple init blocks and they run in order of appearance in the class.

    initCommands() is a function defined in the current REPL class.
     */
    init {
        initCommands()
    }

    /*
    command() processes a single REPL command.
    It takes the command name (str) and its parameters (param), returns whether REPL should exit.

    For example, user types "read myfile.smol" in the REPL, the command() function processes it through commands["read"] and "myfile.smol" is the parameter for calling execute() method on a Command object. In the command map, commands["read"] leads to an instance of Command class, where the "command" function is defined as: { str -> initInterpreter(str); false }. Therefore, through the execute() method, "myfile.smol" is parsed as "param" in command(param), which now becomes "str" in initInterpreter(str).
     */
    fun command(str: String, param: String): Boolean {
        // returns `true` if REPL should exit
        val start = LocalTime.now()
        val result: Boolean
        if (str == "help") {
            for (cmd in commands.values.distinct()) {
                println(String.format("%-11s - %s", cmd.name, cmd.help))
                if (cmd.parameterHelp != "") {
                    println(String.format("%14s- parameter: %s", "", cmd.parameterHelp))
                }
            }
            result = false
        // If not help, looks up command in commands map. If command not found, prints error message.
        } else {
            val command = commands[str]
            if (command == null) {
                printRepl("Unknown command $str. Enter \"help\" to get a list of available commands.")
                result = false
            } else {
                /*
                If command found:
                - Checks if interpreter needed but not initialized
                - Otherwise executes command in try-catch
                - Handles any errors with appropriate messages
                 */
                if (interpreter == null && command.requiresFile) {
                    printRepl("No file loaded. Please \"read\" a file to continue.")
                    result = false
                } else {
                    result = try {
                        command.execute(param)
                    } catch (e: Exception) {
                        printRepl("Command $str $param caused an exception. Internal state may be inconsistent.")
                        if (settings.verbose) e.printStackTrace() else printRepl("Trace suppressed, set the verbose flag to print it.")
                        false
                    }
                }
            }
        }
        // If verbose mode on and command did not exit REPL, prints execution time. Returns result.
        if (settings.verbose && !result) {
            val elapsedTime = Duration.between(start, LocalTime.now())
            printRepl("Evaluation took ${elapsedTime.seconds}.${elapsedTime.nano} seconds")
        }
        return result
    }

    private fun dump(file: String) {
        interpreter!!.dump(file)
    }

    fun runAndTerminate() {
        while (!interpreter!!.stack.empty() && interpreter!!.makeStep());
    }

    fun printRepl(str: String) {
        println("MO-out> $str \n")
    }

    // The path parameter points to a SMOL source code file with .smol extension.
    private fun initInterpreter(path: String) {
        /*
        Load and read the contents of a standard library file for SMOL
        - this::class - References the current class's Kotlin class object (REPL in this case)
        - .java - Converts the Kotlin class reference to a Java class reference
        - .classLoader - Gets the ClassLoader that loaded this class. A ClassLoader is responsible for loading Java classes and resources.
        - .getResource("StdLib.smol") - Looks up the file "StdLib.smol" in the classpath resources.
        - .readText() - Reads the entire content of the found resource file as text
         */
        val stdLib = this::class.java.classLoader.getResource("StdLib.smol").readText()
        // Reads the user's program file content
        val program = File(path).readText(Charsets.UTF_8)
        /*
        Create a lexical analyzer (lexer) for combined program (user program + standard library)
        - program + "\n\n" + stdLib - Combines the user's program text with the standard library text, separated by two newlines
        - CharStreams.fromString() - Creates an input stream from the combined text. CharStreams is an ANTLR utility class that converts text into a form that ANTLR lexers can process.
        - WhileLexer() - Creates a new lexer using the generated WhileLexer class (which is generated by ANTLR from a grammar definition).
        - WhileLexer and CharStreams come from ANTLR-generated code based on While.g4 grammar. ANTLR code generation is configured through build.gradle

        The lexer's job is to break down the input text into tokens - the basic units of the language like:
        - Keywords (if, while, class, etc.)
        - Identifiers (variable names, function names)
        - Operators (+, -, =, etc.)
        - Numbers
        - Strings
        - Special characters ({, }, ;, etc.)

        This tokenized form is what the parser will later use to understand the structure of the program. The lexer is the first step in processing the SMOL code from text into a form that can be executed.

        Why lexer?
        - Imagine we have this SMOL code: x := 5 + 10
        - The computer cannot directly understand this text. It needs to be broken down into meaningful pieces that can be processed. This is where a lexer comes in.

        Why concatenate StdLib.smol and the user SMOL program?
        1. Dependency Resolution: The user's SMOL program might use classes, functions, or types defined in the standard library.
        2. Single Pass Processing: It is more efficient to run the lexer once on the combined code.
        3. Scope and Context: The standard library provides the base context in which user code runs. Having them tokenized together ensures proper scope resolution.
         */
        val lexer = WhileLexer(CharStreams.fromString(program + "\n\n" + stdLib))
        /*
        Create token stream from lexer (from ANTLR). Buffer all tokens from the lexer and provides access to them.
        - CommonTokenStream comes from ANTLR runtime library org.antlr.v4.runtime.CommonTokenStream
         */
        val tokens = CommonTokenStream(lexer)
        /*
        Creates parser with tokens (from ANTLR) that will consume the token stream.
        - WhileParser comes from ANTLR-generated code based on While.g4 grammar.
         */
        val parser = WhileParser(tokens)
        /*
        Parses the tokens according to the "program" rule in the grammar to return an Abstract Syntax Tree (AST) representing the program structure.
        - program() is a method generated by ANTLR from the While.g4 grammar.
         */
        val tree = parser.program()
        /*
        Extend ANTLR's visitor pattern to traverse the AST and translate the AST into the interpreter's internal representation
        - "Translate" is a custom class defined in Translate.kt in this project.
         */
        val visitor = Translate()
        /*
        Takes the AST and generates:
        - Initial program state (StackEntry)
        - Static program information (StaticTable)
        Returns these as a Pair

        - generateStatic is a method in the Translate class.
         */
        val pair = visitor.generateStatic(tree)

        /*
        Manages the RDF triples for semantic data querying.
        Required to set up type checking and semantic features.
        - Creates a TripleManager instance from TripleManager.kt
         */
        val tripleManager = TripleManager(settings, pair.second, null)
        /*
        Verifies all types in the program are correct.
        Required to catch type errors before running the program.
        - Creates a TypeChecker instance from TypeChecker.kt
         */
        val tC = TypeChecker(tree, settings, tripleManager)
        /*
        Performs the type checking on the program to validate program is type-safe.
        - Calls check() method defined in TypeChecker.kt
         */
        tC.check()
        /*
        Outputs any type errors found during checking.
        - Calls report() method defined in TypeChecker.kt
         */
        tC.report()

        /*
        Provides initial program memory state.
        - Creates a mutable map using Kotlin's mutableMapOf(), with:
        --- Key: A LiteralExpr (pair.first.obj)
        --- Value: An empty Memory (the inner mutableMapOf())

        - "initGlobalStore: GlobalMemory" declares initGlobalStore is of type GlobalMemory, which means it is a mutable map, where:
        --- GlobalMemory and Memory are defined in State.kt as type aliases.
        --- Keys are LiteralExpr objects (representing program objects).
        --- Values are Memory objects (which are themselves mutable maps from String to LiteralExpr).

        - "pair.first.obj"
        --- pair.first is a StackEntry containing the program's entry object. The "pair" comes from visitor.generateStatic(tree) which returns a Pair<StackEntry, StaticTable>.
        --- pair.first.obj contains the LiteralExpr representing this entry object. Since StackEntry has a field obj of type LiteralExpr, pair.first.obj is a LiteralExpr.
        --- StackEntry is defined in State.kt. LiteralExpr is defined in LiteralExpr.kt. StackEntry and LiteralExpr are Kotlin data classes.
         */
        val initGlobalStore: GlobalMemory = mutableMapOf(Pair(pair.first.obj, mutableMapOf()))
        /*
        Creates a new Stack object from Java's util.Stack class.
        The <StackEntry> means this Stack can only store StackEntry objects.
        StackEntry is our data class with fields: active, store, obj, and id.
        The stack will be used to track the program execution states.
         */
        val initStack = Stack<StackEntry>()
        /*
        pair.first is the StackEntry instance we got from visitor.generateStatic(tree). This becomes the first (and currently only) execution state on the stack.
         */
        initStack.push(pair.first)
        // Creates new Interpreter instance with all initialized components.
        interpreter = Interpreter(
            initStack,
            initGlobalStore,
            mutableMapOf(),
            pair.second,
            settings
        )
    }

    /*
    Set up all available commands in the REPL.

    "commands" is a mutable map where:
    - The key is a String (like "exit", "read", "step")
    - The value is a Command instance.
     */
    private fun initCommands() {
        // Create exit command that returns true to end REPL.
        commands["exit"] = Command("exit", this, { true }, "exits the shell", requiresFile = false)

        /*
        File loading commands: read and reada.
        - read: Loads file but does not run it.
        - reada: Loads and automatically runs the file.
         */
        commands["read"] = Command(
            "read",
            this,
            // "str" is the file path following the "read"
            { str -> initInterpreter(str); false },
            "reads a file",
            requiresFile = false,
            parameterHelp = "Path to a .smol file",
            requiresParameter = true
        )
        commands["reada"] = Command(
            "reada",
            this,
            { str -> initInterpreter(str); while (interpreter!!.makeStep()); false },
            "reads a file and runs auto",
            parameterHelp = "Path to a .smol file",
            requiresParameter = true,
            requiresFile = false
        )

        // Commands to display program information and state.
        commands["info"] = Command(
            "info",
            this,
            { printRepl(interpreter!!.staticInfo.toString()); false },
            "prints static information in internal format"
        )
        val examine =
            Command("examine", this, { printRepl(interpreter!!.toString()); false }, "prints state in internal format")
        commands["examine"] = examine
        commands["e"] = examine

        /*
        File Output Commands:
        - dump: writes the current program state as RDF triples to a file. If no filename is provided, it uses "output.ttl". The file is created in the current output directory.
        - outdir: shows the current output directory (if no parameter given) or changes the output directory to a new path (if parameter given)
         */
        commands["dump"] =
            Command(
                "dump",
                /*
                this:
                - In Kotlin, "this" refers to the current REPL instance.
                - When creating a Command object, it needs a reference to its current REPL instance to call REPL methods (like printRepl).
                 */
                this,
                // This is the "command" parameter.
                { str ->                // Lambda function that executes when command runs
                    val file = if (str == "") "output.ttl" else str     // If no filename given, use "output.ttl"
                    dump(file); false   // Call REPL's dump function with filename. Then, Return false to continue REPL execution.
                },
                "dumps into \${outdir}/\${file}",
                parameterHelp = "file: filename, default \"output.ttl\""
            )
        commands["outdir"] = Command(
            "outdir",
            this,
            { str ->
                if (str == "") {
                    printRepl("Current output directory is ${settings.outdir}")
                } else {
                    settings.outdir = str
                }
                false
            },
            "sets or prints the output path",
            parameterHelp = "path (optional): the new value of outdir",
        )

        /*
        Execution Control Commands:
        - auto: keeps executing steps until there are no more steps to execute.
        - step: execute exactly one step.

        The makeStep() function comes from the Interpreter class in Interpreter.kt. It executes exactly one step of the interpreter program, and returns true if another step can be executed.
         */
        commands["auto"] = Command(
            "auto",
            this,
            /*
            The "!!" is Kotlin's non-null assertion operator. It means:
            - "I am certain interpreter is not null here."
            - If interpreter is null, it will throw NullPointerException.
            - It is used because interpreter is declared as nullable (Interpreter?)
             */
            { while (interpreter!!.makeStep()); false },
            "continues execution until the next breakpoint"
        )
        val step = Command(
            "step",
            this,
            { interpreter!!.makeStep(); false },
            "executes one step"
        )
        commands["step"] = step
        commands["s"] = step

        commands["guards"] = Command(
            "guards",
            this,
            { str ->
                val p1 = listOf("heap", "staticTable")
                val p2 = listOf("true", "false")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 2) {
                    printRepl("\n" + "This command requires exactly two parameters.")
                } else {
                    if (!p1.contains(p[0])) {
                        printRepl("\nFirst parameter must one of: $p1")
                    }
                    if (!p2.contains(p[1])) {
                        printRepl("\nSecond parameter must one of: $p2")
                    }
                    if (p1.contains(p[0]) && p2.contains(p[1])) {
                        interpreter!!.tripleManager.currentTripleSettings.guards[p[0]] = (p[1] == "true")
                        printRepl("Guard clauses in ${p[0]} set to: ${p[1]}")
                    }
                }
                false
            },
            "Enables/disables guard clauses when searching for triples in the heap or the static table",
            parameterHelp = "[heap|staticTable] [true|false]",
            requiresParameter = true
        )

        commands["virtual"] = Command(
            "virtual",
            this,
            { str ->
                val p1 = listOf("heap", "staticTable")
                val p2 = listOf("true", "false")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 2) {
                    printRepl("\n" + "This command requires exactly two parameters.")
                } else {
                    if (!p1.contains(p[0])) {
                        printRepl("\nFirst parameter must one of: $p1")
                    }
                    if (!p2.contains(p[1])) {
                        printRepl("\nSecond parameter must one of: $p2")
                    }
                    if (p1.contains(p[0]) && p2.contains(p[1])) {
                        interpreter!!.tripleManager.currentTripleSettings.guards[p[0]] = (p[1] == "true")
                        printRepl("Virtualization for ${p[0]} set to: ${p[1]}")
                    }
                }
                false
            },
            "Enables/disables virtualization searching for triples in the heap or the static table. Warning: the alternative to virtualization is naive and slow.",
            parameterHelp = "[heap|staticTable] [true|false]",
            requiresParameter = true
        )

        commands["source"] = Command(
            "source",
            this,
            { str ->
                val p1 = listOf("heap", "staticTable", "vocabularyFile", "externalOntology")
                val p2 = listOf("true", "false")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 2) {
                    printRepl("\n" + "This command requires exactly two parameters.")
                } else {
                    if (!p1.contains(p[0])) {
                        printRepl("\nFirst parameter must one of: $p1")
                    }
                    if (!p2.contains(p[1])) {
                        printRepl("\nSecond parameter must one of: $p2")
                    }
                    if (p1.contains(p[0]) && p2.contains(p[1])) {
                        interpreter!!.tripleManager.currentTripleSettings.sources[p[0]] = (p[1] == "true")
                        printRepl("Use source ${p[0]} set to ${p[1]}")
                    }
                }
                false
            },
            "Set which sources to include (true) or exclude (false) when querying",
            parameterHelp = "[heap|staticTable|vocabularyFile|externalOntology] [true|false]",
            requiresParameter = true
        )

        commands["reasoner"] = Command(
            "reasoner",
            this,
            { str ->
                val allowedParameters = listOf("off", "rdfs", "owl")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 1) {
                    printRepl("\n" + "This command requires exactly one parameter.")
                } else {
                    if (!allowedParameters.contains(p[0])) {
                        printRepl("\nParameter must one of: $allowedParameters")
                    } else {
                        interpreter!!.tripleManager.currentTripleSettings.jenaReasoner = p[0]
                        printRepl("Reasoner changed to: ${p[0]}")
                    }
                }
                false
            },
            "Specify which Jena reasoner to use, or turn it off",
            parameterHelp = "[off|rdfs|owl]",
            requiresParameter = true
        )

        val query = Command(
            "query",
            this,
            { str ->
                val results = interpreter!!.query(str)
                printRepl("\n" + ResultSetFormatter.asText(results))
                false
            },
            "executes a SPARQL query",
            parameterHelp = "SPARQL query",
            requiresParameter = true
        )
        commands["query"] = query
        commands["q"] = query

        commands["plot"] = Command(
            "plot",
            this,
            { str ->
                val params = str.split(" ")
                if (params.size != 4 && params.size != 2) {
                    printRepl("plot expects 2 or 4 parameters, separated by blanks, got ${params.size}.")
                } else {
                    val q = if (params.size == 4)
                        "SELECT ?at ?val WHERE { ?m smol:roleName \"${params[0]}\"; smol:ofPort [smol:withName \"${params[1]}\"]; smol:withValue ?val; smol:atTime ?at. FILTER (?at >= ${params[2]} && ?at <= ${params[3]}) }  ORDER BY ASC(?at)"
                    else "SELECT ?at ?val WHERE { ?m smol:roleName \"${params[0]}\"; smol:ofPort [smol:withName \"${params[1]}\"]; smol:withValue ?val; smol:atTime ?at }  ORDER BY ASC(?at)"
                    val results = interpreter!!.query(q)

                    printRepl("Executed $q")
                    if (results != null) {
                        var out = ""
                        for (r in results) {
                            out += r.getLiteral("at").double.toString() + "\t" + r.getLiteral("val").double.toString() + "\n"
                        }

                        val output = File("${settings.outdir}/plotting.tsv")
                        if (!output.exists()) output.createNewFile()
                        output.writeText(out)
                        val output2 = File("${settings.outdir}/plotting.gp")
                        if (!output2.exists()) output.createNewFile()
                        output2.writeText("set terminal postscript \n set output \"${settings.outdir}/out.ps\" \n plot \"${settings.outdir}/plotting.tsv\" with linespoints")


                        val rt = Runtime.getRuntime()
                        val proc = rt.exec("which gnuplot")
                        proc.waitFor()
                        val exitVal = proc.exitValue()
                        val proc2 = rt.exec("which atril")
                        proc2.waitFor()
                        val exitVal2 = proc2.exitValue()
                        if (exitVal != 0 || exitVal2 != 0) {
                            printRepl("Cannot find gnuplot or atril, try to plot and display the files in ${settings.outdir} manually.")
                        } else {
                            printRepl("Plotting....")
                            Runtime.getRuntime().exec("gnuplot ${settings.outdir}/plotting.gp")
                            Runtime.getRuntime().exec("atril ${settings.outdir}/out.ps")
                            printRepl("Finished. Generated files are in ${settings.outdir}.")
                        }
                    }
                }
                false
            },
            "plot ROLE PORT FROM TO runs gnuplot on port PORT of role ROLE from FROM to TO. FROM and TO are optional",
            requiresParameter = true
        )
        commands["consistency"] = Command(
            "consistency",
            this,
            { _ ->
                val ontology = interpreter!!.tripleManager.getOntology()
                val reasoner: OWLReasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
                ontology.classesInSignature().forEach { println(it) }
                printRepl("HermiT result ${reasoner.isConsistent}")
                false
            },
            "prints all classes and checks that the internal ontology is consistent"
        )
        commands["class"] = Command(
            "class",
            this,
            { str ->
                var outString = "Instances of $str:\n"
                for (node in interpreter!!.owlQuery(str)) {
                    // N node can appearently have more than one entity. Print all entities in all nodes.
                    var prefix = ""
                    for (entity in node.entities) {
                        outString = outString + prefix + "$entity"
                        prefix = ", "
                    }
                    outString += "\n"
                }
                printRepl(outString)
                false
            },
            "returns all members of a class",
            parameterHelp = "class expression in Manchester Syntax, e.r., \"<smol:Class>\"",
            requiresParameter = true
        )
        commands["eval"] = Command(
            "eval",
            this,
            { str ->
                val lexer2 = WhileLexer(CharStreams.fromString(str))
                val tokens2 = CommonTokenStream(lexer2)
                val parser2 = WhileParser(tokens2)
                val tree2 = parser2.expression()
                val visitor2 = Translate()
                val newExpr = visitor2.visit(tree2) as Expression

                printRepl(interpreter!!.evalTopMost(newExpr).literal)
                false
            },
            "evaluates a .smol expression in the current frame",
            parameterHelp = "a .smol expression",
            requiresParameter = true,
        )
        commands["verbose"] = Command(
            "verbose",
            this,
            { str ->
                if (str == "on" || str == "true") {
                    settings.verbose = true
                } else if (str == "off" || str == "false") {
                    settings.verbose = false
                }
                false
            },
            "Sets verbose output to on or off",
            parameterHelp = "`true` or `on` to switch on verbose output, `false` or `off` to switch it off",
            requiresParameter = true,
            requiresFile = false
        )
    }

    fun terminate() {
        interpreter?.terminate()
    }
}
