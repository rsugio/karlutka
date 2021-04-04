package k2

import org.xmlet.xsdparser.core.XsdParser
import java.io.File

class XsdToGroovyGen {
    val idocParser: XsdParser
    constructor(f: File) {
        idocParser = XsdParser(f.path)
    }

    fun gen() {
        val el = idocParser.resultXsdElements.findFirst()
        if (el.isEmpty) {
            // TODO
            System.err.println("В XSD айдока нет корневого элемента")
            return
        }
        val out = StringBuilder()
        val root = el.get()
        assert (root.minOccurs==1 && root.maxOccurs=="1")
        val d = root.annotation.documentations.joinToString(separator = "\n") { xsdoc -> xsdoc.content }
        out.append("${root.name} { //$d\n")
        val z = root.xsdComplexType

        out.append("}\n")
        println(out)
    }
}