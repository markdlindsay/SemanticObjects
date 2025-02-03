/*
- Represents an OWL statement (a single, complete instruction or action that the program should execute) in the SMOL language.
- Used to perform OWL queries and assign the results to a target variable.
- Deals with the knowledge representation aspect of the program by interacting with the ontology.
 */

package no.uio.microobject.ast.stmt // This is a package declaration that organises code into namespaces. The package structure indicates this code is part of the abstract syntax tree (ast) handling statements (stmt). The no.uio prefix suggests this is developed at the University of Oslo. The package is located under the `microobjects` project namespace.

import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type
import org.semanticweb.owlapi.model.OWLNamedIndividual  // Import the OWL API class that represents named individuals in OWL ontologies. Named individuals are specific instances/objects in an ontology. This interface provides method to get the IRI of the individual, access the individual's properties, query relationships with other individuals, and check class membership.
import org.semanticweb.owlapi.reasoner.NodeSet  // Import the OWL API class that represents sets of nodes from reasoning results. NodeSet is a collection type specifically designed for OWL reasoning outputs, which is used to store and process results from ontology queries. In the current context, this is used to handle the results returned by OWL queries about geological features.


data class OwlStmt(val target : Location, val query: Expression, val pos : Int = -1, val declares: Type?) : Statement {
    /*
        Define a data class called OwlStmt which represents an OWL statement in the language.
        It has properties:
        - target: a Location representing where the result will be stored
        - query: an Expression representing the OWL query
        - pos: an optional Int representing the line position, defaulting to -1
        - declares: an optional Type representing the declared return type
     */

    override fun toString(): String = "$target := member($query)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:OwlStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        if (query !is LiteralExpr || query.tag != STRINGTYPE) {
            throw Exception("Please provide a string as the input to a derive statement")
        }

        val res : NodeSet<OWLNamedIndividual> = interpreter.owlQuery(query.literal)
        var list = LiteralExpr("null")
        for (r in res) {
            val name = Names.getObjName("List")
            val newMemory: Memory = mutableMapOf()
            val found = r.toString().removePrefix("Node( <").split("#")[1].removeSuffix("> )")

            val foundAny = interpreter.heap.keys.firstOrNull { it.literal == found }
            if(foundAny != null) newMemory["content"] = LiteralExpr(found, foundAny.tag)
            else {
                if(found.startsWith("\"")) newMemory["content"] = LiteralExpr(found, STRINGTYPE)
                else if(found.matches("\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, INTTYPE)
                else if(found.matches("\\d+.\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, DOUBLETYPE)
                else throw Exception("Concept returned unknown object/literal: $found")
            }

            newMemory["next"] = list
            interpreter.heap[name] = newMemory
            list = name
        }
        return replaceStmt(AssignStmt(target, list, declares = declares), stackFrame)
    }
}