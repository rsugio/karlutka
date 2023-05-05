package karlutka.server

import karlutka.models.MPI
import karlutka.parsers.pi.SimpleQuery
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

object SPROXY {
    // SPROXY - наш!
    val workspaces = mutableListOf<Workspace>()
    var cnt: Int = 1
    var tracedir = Paths.get("c:/data/tmp/SPROXY")

    @Serializable
    data class Objects(
        val workspace: Workspace,
        val namespaces: MutableMap<String, String> = mutableMapOf(),
    ) {
        fun encodeToString() = Json.encodeToString(this)
    }

    @Serializable
    data class Workspace(
        val RA_WORKSPACE_ID: String,
        val WS_NAME: String,
        val VENDOR: String,
        val NAME: String,
        val VERSION: String,
        val CAPTION: String,
        val WS_TYPE: Char,
        val ORIGINAL_LANGUAGE: String,
        val DEPWS_ID: String
    ) {
        fun encodeToString() = Json.encodeToString(this)
        fun toR() = SimpleQuery.R(
            listOf(
                SimpleQuery.C(SimpleQuery.WkID(RA_WORKSPACE_ID, -1)),
                SimpleQuery.C(WS_NAME),
                SimpleQuery.C(VENDOR),
                SimpleQuery.C(NAME),
                SimpleQuery.C(VERSION),
                SimpleQuery.C(CAPTION),
                SimpleQuery.C(WS_TYPE),
                SimpleQuery.C(ORIGINAL_LANGUAGE),
                SimpleQuery.C(null, SimpleQuery.Simple(null, null, null, null, DEPWS_ID)),
            )
        )
    }

    // -----------------------------------------------------------------------------------------------
    fun load(rootPath: Path) {
        workspaces.clear()
        rootPath.listDirectoryEntries().forEach { px ->
            val r = px.resolve("objects.json").readText()
            val objects = decodeFromString(r)
            workspaces.add(objects.workspace)
        }
    }

    fun handle(sreq: String): String {
        val pr = tracedir.resolve("${cnt++}_request.xml")
        pr.writeText(sreq)
        val req = SimpleQuery.decodeFromStringRequest(sreq)
        var rez: String
        val cds = req.result.attrib.mapIndexed { i, s -> SimpleQuery.Def(s, i) }
        val ks = listOf(SimpleQuery.KeyElem("WS_ID", 0), SimpleQuery.KeyElem("WS_ORDER", 1))
        val typei = SimpleQuery.TypeInfo(listOf(SimpleQuery.Type(MPI.ETypeID.workspace, ks)))
        val pn: Path
        if (req.hasWorkspace() && req.condition == SimpleQuery.conditionWS_TYPE_S) {
            println("handler(get all wksp) $pr")
            val header = SimpleQuery.HeaderInfo(workspaces.size, cds.size, SimpleQuery.ColDef(cds))
            val matrix = SimpleQuery.Matrix(workspaces.map { it.toR() })
            rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
            pn = Paths.get("C:\\data\\tmp\\SPROXY\\001.xml")
        } else if (req.hasWorkspace()) {
            println("handler(get single wksp) $pr")
            val name = req.condition.complex!!.getSingle("NAME")
            val version = req.condition.complex.getSingle("VERSION")
            require(name.op == SimpleQuery.EOps.EQ && version.op == SimpleQuery.EOps.EQ)
            //workspaces.find{it.}
            val header = SimpleQuery.HeaderInfo(0, 0, SimpleQuery.ColDef(cds))
            val matrix = SimpleQuery.Matrix() //workspaces.map { it.toR() })
            rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
            pn = Paths.get("C:\\data\\tmp\\SPROXY\\002.xml")
        } else {
            System.err.println("handler(UNIMPLEMENTED) $pr")
            rez = "<UNIMPLEMENTED/>"
            pn = Paths.get("C:\\data\\tmp\\SPROXY\\000.xml")
        }
//        rez = rez.replace("""<headerInfo>""", """<headerInfo xmlns="">""")  //ABAP dummy req
//        rez = rez.replace("""<typeInfo>""", """<typeInfo xmlns="">""")
//        rez = rez.replace("""<matrix>""", """<matrix xmlns="">""")
//        rez = rez.replace("""<matrix/>""", """<matrix xmlns=""/>""")
        val pt = tracedir.resolve("${cnt++}_response.xml")
        pt.writeText(rez)
        if (Files.isRegularFile(pn)) {
            println("Sent substitute $pn instead of $pt")
            rez = pn.readText()
        }
        return rez
    }

    fun decodeFromString(s: String) = Json.decodeFromString<Objects>(s)

    @OptIn(ExperimentalSerializationApi::class)
    fun decodeFromPath(p: Path) = Json.decodeFromStream<Objects>(p.inputStream())


}