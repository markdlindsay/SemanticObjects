package no.uio.microobject.main

import com.github.ajalt.clikt.core.CliktCommand     // Base class for CLI command handling
import com.github.ajalt.clikt.parameters.options.*  // Handles command line options
import com.github.ajalt.clikt.parameters.types.path // Path parameter type support
import no.uio.microobject.runtime.REPL              // The REPL implementation
import java.io.File                                 // File handling
import java.nio.file.Paths                          // Path handling
import kotlin.system.exitProcess                    // For program termination
import org.jline.reader.LineReaderBuilder           // For building the interactive console reader.


/*
The Settings class holds configuration for the semantic framework.

The "data" keyword in Kotlin:
- Makes it a data class, which automatically provides:
    - equals() for comparing instances
    - hashCode() for use in hash-based collections
    - toString() for readable string representation
    - copy() for making modified copies
    - Component functions for destructuring
- Useful for classes that mainly hold data
- In this case, Settings holds configuration data, so making it a data class gives us useful functionality for handling these settings
 */
data class Settings(
    var verbose: Boolean,      //Verbosity
    val materialize: Boolean,  //Materialize
    var outdir: String,        //path of temporary outputs
    val background: String,    // handles the geological ontology and domain knowledge
    /*
    Various URI prefixes needed for semantic handling
    progPrefix, runPrefix, and langPrefix are given default values if not specify
     */
    // For geological domain model elements, used to identify geological concepts and properties
    val domainPrefix: String,
    // For program elements, used to identify elements created by the SMOL program
    val progPrefix: String = "https://github.com/Edkamb/SemanticObjects/Program#",
    // For runtime elements, including current timestamp to make each run unique, used to identify objects created during program execution
    val runPrefix: String = "https://github.com/Edkamb/SemanticObjects/Run${System.currentTimeMillis()}#",
    // For language elements
    val langPrefix: String = "https://github.com/Edkamb/SemanticObjects#",
    val extraPrefixes: HashMap<String, String>,
    val useQueryType: Boolean = false
) {
    var prefixMapCache: HashMap<String, String>? = null // A cache field for prefix mapping

    /*
    prefixMap, replaceKnownPrefixes, replaceKnownPrefixesNoColon, prefixes, and getHeader are all helper functions.
     */
    // Create and cache a map of all prefixes
    fun prefixMap(): HashMap<String, String> {
        // Return cached map if it exists
        if (prefixMapCache != null) return prefixMapCache as HashMap<String, String>
        // Create new map with standard semantic web prefixes
        prefixMapCache = hashMapOf(
            "domain" to domainPrefix,
            "smol" to langPrefix,
            "prog" to progPrefix,
            "run" to runPrefix,
            "owl" to "http://www.w3.org/2002/07/owl#",
            "rdf" to "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "rdfs" to "http://www.w3.org/2000/01/rdf-schema#",
            "xsd" to "http://www.w3.org/2001/XMLSchema#"
        )
        /*
        Add any extra prefixes provided
        prefixMapCache!! - The !! is Kotlin's null assertion operator。
        - It tells the compiler "I am certain this is not null"
        - In this context, we can be certain because we just created prefixMapCache in the lines above。
        - If prefixMapCache were null, this would throw a NullPointerException。
        .putAll(extraPrefixes) - This adds all entries from extraPrefixes to the cache。
        - extraPrefixes is a HashMap<String, String> parameter passed to the Settings class。
        - putAll() copies all key-value pairs from one map to another
        - If extraPrefixes contains any keys that already exist in prefixMapCache, they will be overwritten。
         */
        prefixMapCache!!.putAll(extraPrefixes)
        return prefixMapCache as HashMap<String, String>
    }

    // Replaces short prefix names with full URIs.
    fun replaceKnownPrefixes(string: String): String {
        // Replaces any occurrence of "domain:" with the value of domainPrefix plus a colon.
        var res = string.replace("domain:", "$domainPrefix:")
            // Chains another replacement for "prog:" prefix.
            .replace("prog:", "$progPrefix:")
            .replace("run:", "$runPrefix:")
            .replace("smol:", "$langPrefix:")
        for ((k, v) in extraPrefixes) res = res.replace("$k:", "$v:")
        return res
    }

    /*
    Replaces short prefix names for HermiT parser.
    Regular format: domain:Rock -> https://domain/path#:Rock
    HermiT format: domain:Rock -> https://domain/path#Rock
    HermiT is imported in QueryChecker.kt through:
    "
    import org.semanticweb.HermiT.Configuration
    import org.semanticweb.HermiT.Reasoner
    "
     */
    fun replaceKnownPrefixesNoColon(string: String): String { //For the HermiT parser, BEWARE: requires that the prefixes and in #
        var res = string.replace("domain:", domainPrefix)
            .replace("prog:", progPrefix)
            .replace("run:", runPrefix)
            .replace("smol:", langPrefix)
        for ((k, v) in extraPrefixes) res = res.replace("$k:", v)
        return res
    }

    fun prefixes(): String {
        // var res = """ - Starts a multi-line string with triple quotes.
        var res = """@prefix smol: <${langPrefix}> .
           @prefix prog: <${progPrefix}> .
           @prefix domain: <${domainPrefix}> .
           @prefix run: <${runPrefix}> .""".trimIndent()    // .trimIndent() - Removes common indentation from all lines
        for ((k, v) in extraPrefixes) res = "$res\n@prefix $k: <$v>."
        return res + "\n"
    }

    fun getHeader(): String {
        return """
        @prefix owl: <http://www.w3.org/2002/07/owl#> .
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
        """.trimIndent()
    }
}

/*
Handling command-line interactions and setting up the SMOL environment.
The Main class extends CliktCommand, which uses the Clikt library for command line argument handling.
Clikt is a Kotlin library for creating command line interfaces.
 */
class Main : CliktCommand() {
    private val mainMode by option().switch(
        /*
        The program can be run in either "execute" mode or "repl" mode.
        Users can specify this using either --execute/-e or --load/-l.
        If no mode is specified, it defaults to "repl".

        REPL (Read-Eval-Print Loop) mode: Interactive development and testing
        - Starts an interactive shell with prompt "MO> "
        - Accepts commands one at a time
        - Splits each input into command and parameters
        - Executes each command and waits for the next input
        - Continues until a termination command is given

        Execute mode: Automated processing of SMOL files
        - Runs the input SMOL file directly
        - Processes the entire file without interactive input
        - Terminates after completion
         */
        "--execute" to "execute", "-e" to "execute",
        "--load" to "repl", "-l" to "repl",
    ).default("repl")

    /*
    This defines an option for specifying a path to an OWL file containing background knowledge/definitions. Users can use either --back or -b followed by the file path.

    The .path() method is a utility function from the Clikt library that processes the command line input into a Path object.
    - It converts the string input (file path) provided by the user into a Java Path object.
    - It validates that the input can be interpreted as a valid file path.
    - It provides type safety by ensuring the back property will contain a Path object rather than just a string.

    For example, if a user runs the program with "--back /path/to/ontology.owl", the .path() method will:
    1. Take the string "/path/to/ontology.owl".
    2. Convert it to a proper Path object representing that filesystem location.
    3. Store that Path object in the "back" property.
    This is more robust than handling raw strings because:
    1. It validates path syntax.
    2. It handles different operating system path formats.
    3. It provides proper path manipulation methods through the Path API.
     */
    private val back by option(
        "--back",
        "-b",
        help = "path to a file containing OWL class definitions as background knowledge."
    ).path()
    private val domainPrefix by option(
        "--domain",
        "-d",
        help = "prefix for domain:."
    ).default("https://github.com/Edkamb/SemanticObjects/ontologies/default#")
    private val input by option("--input", "-i", help = "path to a .smol file which is loaded on startup.").path()
    private val replay by option("--replay", "-r", help = "path to a file containing a series of REPL commands.").path()
    private val outdir by option("--outdir", "-o", help = "path to a directory used to create data files.").path()
        .default(Paths.get("").toAbsolutePath())
    private val verbose by option("--verbose", "-v", help = "Verbose output.").flag()
    private val materialize by option("--materialize", "-m", help = "Materialize triples and dump to file.").flag()
    private val queryType by option("--useQueryType", "-q", help = "Activates the type checker for access").flag()

    /*
    This option allows defining additional prefix mappings for URIs. Users can specify multiple prefix-URI pairs.

    The .associate() method in this context transforms multiple command-line arguments into a key-value map.

    For example, "-p PREFIX1=URI1 -p PREFIX2=URI2", the .associate() method:
    1. Takes each argument string
    2. Splits it at the "=" character
    3. Creates a Map where:
        - Keys are the prefix names (PREFIX1, PREFIX2)
        - Values are the URIs (URI1, URI2)
     */
    private val extra by option(
        "--prefixes",
        "-p",
        help = "Extra prefixes, given as a list -p PREFIX1=URI1 -p PREFIX2=URI2"
    ).associate()

    override fun run() {
        /*
        Initialize the Apache Jena query engine with org.apache.jena.query.ARQ.init().
        Jena is a Java framework for building Semantic Web applications, and is used here for querying the ontology.
         */
        org.apache.jena.query.ARQ.init()

        /*
        Check if a path to a background knowledge file was provided with the --back or -b option.
        - If so, it attempts to read the contents of this file into the "backgr" variable.
        - If the file does not exist, it prints an error message.
         */
        var backgr = ""
        if (back != null) {
            /*
            "File" is a class from the Kotlin standard library (specifically, from the java.io package) that represents a file or directory path name. It provides methods for reading from and writing to files.

            If "back" is not null, it creates a new "File object from the "back" path by calling back.toString(). The toString() method is called because the "File" constructor expects a string, not a "Path".
             */
            val file = File(back.toString())
            if (file.exists()) {
                // If the file exists, it reads the entire contents of the file into a string using file.readText(), and assigns this string to the "backgr" variable.
                backgr = file.readText()
            } else println("Could not find file for background knowledge: ${file.path}")
        }

        // If no input file was specified with the --input or -i option, and the mainMode is not "repl", it prints an error message and exits.
        if (input == null && mainMode != "repl") {
            println("Error: please specify an input .smol file using \"--input\".")
            exitProcess(-1)
        }

        // "REPL" class is defined in the REPL.kt file.
        val repl = REPL(
            Settings(
                verbose,
                materialize,
                outdir.toString(),
                backgr,
                domainPrefix,
                extraPrefixes = HashMap(extra),
                useQueryType = queryType
            )
        )

        // "command" is a function of the "REPL" class.
        if (input != null) {
            repl.command("read", input.toString())
        }

        /*
        If a replay file was specified with the --replay or -r option, it reads each line of this file and passes it to the "command" function of the "REPL" instance, effectively replaying a series of commands.

        The purpose of the "replay" feature in this project is to allow the user to provide a file containing a series of REPL commands, which the application will then execute automatically. This is useful for reproducing a specific scenario, testing a particular sequence of commands, or demonstrating the application's functionality.

        The replay feature could be used to:
        1. Reproduce the experiments or examples described in the paper. The authors could provide replay files corresponding to each experiment, allowing users to easily replicate the results.
        2. Test the application with a predefined set of commands. Developers could create replay files that cover various functionality of the application, and use these files to ensure that the application behaves as expected.
        3. Demonstrate the application's capabilities. The authors or users could create replay files that showcase interesting or important features of the application, and share these files with others.
         */
        if (replay != null) {
            val str = replay.toString()
            /*
            "replay" is a "Path" object representing the path to a file containing a series of REPL commands to replay.
            "str" is a string representation of this path, obtained by calling replay.toString().
            File(str) creates a new "File" object from this string path. The "File" class represents a file on the file system.
             */
            File(str).forEachLine {
                // In the lambda function, "it" is a variable that represents the current line being processed.
                if (!it.startsWith("#") && it != "") {
                    // If the line is not a comment and not empty, it prints the line with the prefix "MO-auto> ".
                    println("MO-auto> $it")
                    /*
                    This line uses the "split" function to divide the current line (it) into parts based on the space character (" "). The limit = 2 argument specifies that the line should be split at most once. This means that if the line contains more than one space, everything after the first space will be considered as part of the second split.
                     */
                    val splits = it.split(" ", limit = 2)
                    /*
                    - If splits.size is 1, it means the line contained no spaces, so there is no argument. In this case, "left" is assigned an empty string ("").
                    - If splits.size is 2 (or more), it means the line contained at least one space. In this case, "left" is assigned the second element of the splits array (splits[1]), which is everything after the first space.
                     */
                    val left = if (splits.size == 1) "" else splits[1]
                    /*
                    splits.first() (which is the first element of the "splits" array) will contain the command, and "left" will contain the argument to the command (or an empty string if there was no argument). These are then passed to the repl.command function to be executed.
                     */
                    repl.command(splits.first(), left)
                }
            }
        }

        if (mainMode == "repl") {
            println("Interactive shell started.")
            // Creates a line reader object using JLine library for handling console input. This provides features like command history and line editing.
            val reader = LineReaderBuilder.builder().build()
            do {
                // Reads a line of input with "MO> " as the prompt. The ?: break means if readLine returns null (e.g., on Ctrl+D), break the loop.
                val next = reader.readLine("MO> ") ?: break
                val splits = next.trim().split(" ", limit = 2)
                val left = if (splits.size == 1) "" else splits[1].trim()
            } while (!repl.command(splits.first(), left))
        } else if (replay == null) {
            // If not in REPL mode and no replay file is specified, runs the program in automatic mode until completion.
            repl.runAndTerminate() //command("auto", "");
        }
        repl.terminate()
    }
}

fun main(args: Array<String>) = Main().main(args)
