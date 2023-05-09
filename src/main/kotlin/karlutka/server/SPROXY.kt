package karlutka.server

import karlutka.models.MPI
import karlutka.parsers.pi.HmUsages
import karlutka.parsers.pi.PCommon
import karlutka.parsers.pi.SimpleQuery
import karlutka.parsers.pi.XiObj
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

object SPROXY {
    // SPROXY - наш!
    private val objectss = mutableListOf<Objects>()
    private var cnt: Int = 1
    private var tracedir = Paths.get("c:/data/tmp/SPROXY")

    @Serializable
    data class Objects(
        val workspace: Workspace,
        val namespaces: MutableMap<String, String> = mutableMapOf(),
    ) {
        @kotlinx.serialization.Transient
        lateinit var folder: Path
        private val nsToPath = mutableMapOf<String, Path>()
        fun encodeToString() = Json.encodeToString(this)

        fun init(folder: Path) {
            this.folder = folder
            require(namespaces.values.toSet().size == namespaces.size, { "Имена неймспейсов должны быть уникальные" })
            namespaces.forEach { k, v ->
                val np = folder.resolve(k)
                if (!Files.exists(np)) {
                    Files.createDirectory(np)
                }
                nsToPath[v] = np
            }
        }

        fun findObjects(criteria: SimpleQuery.RequestByNameNamespaceEQ): List<SimpleQuery.R> {
            val path = nsToPath[criteria.namespace]?.resolve("${criteria.name}.${criteria.entity}")
            if (path != null && Files.exists(path)) {
                val x = XiObj.decodeFromPath(path)
                val lst: List<SimpleQuery.C> = criteria.attrib.map { name ->
                    if (name == "RA_XILINK") {
                        SimpleQuery.C(null, null, SimpleQuery.Qref(false, false, workspace.toRef(x, true)))
                    } else if (name == "TEXT") {
                        SimpleQuery.C("//TODO")
                    } else if (name == "MODIFYUSER") {
                        SimpleQuery.C(x.generic.admInf?.modifBy)
                    } else if (name == "MODIFYDATE") {
                        SimpleQuery.C(null, SimpleQuery.Simple(null, null, null, "2004-02-06T08:59:28"))    //TODO
                    } else if (name == "FOLDERREF") {
                        SimpleQuery.C(null, SimpleQuery.Simple(null, null, null, null, "00000000000000000000000000000000"))
                    } else if (name == "CATEGORY") {
                        SimpleQuery.C("O")      //TODO
                    } else if (name == "IFPATTERN") {
                        SimpleQuery.C("")       //TODO
                    } else if (name == "OBJECTSTATE") {
                        SimpleQuery.C("N")      //TODO
                    } else if (name == "RA_LINK_LIST_ROLE") {
                        SimpleQuery.C(null, null, null, null, listOf(SimpleQuery.QArray(SimpleQuery.Simple("_inner"))))
                    } else if (name == "RA_LINK_LIST") {
                        //SimpleQuery.QArray(null, workspace.toRef(x, false))
                        SimpleQuery.C(null, null, null, null, listOf())
                    } else {
                        System.err.println("(SPROXY:74) Unknown attrib name: $name")
                        TODO()
                    }
                }
                return listOf(SimpleQuery.R(lst))
            }
            return listOf()
        }

        fun namespacesToR(): List<SimpleQuery.R> {
            val x = namespaces.values.map { name ->
                val ref = workspace.toRef(null, false)
                SimpleQuery.Nsp(false, PCommon.Key("namespace", null, listOf(name)), ref)
            }
            return x.map { SimpleQuery.R(listOf(SimpleQuery.C(null, null, null, it))) }
        }
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

        fun toRef(x: XiObj?, withVersion: Boolean): HmUsages.Ref {
            val cl = PCommon.ClCxt('A')
            val vspec: HmUsages.Ref.VSpec? = if (x!=null && withVersion)
                HmUsages.Ref.VSpec(4, x.idInfo.VID!!, false)
            else
                null
            val key = if (x!=null)
                x.idInfo.key
            else {
                // для 'namespace'
                PCommon.Key("swc", RA_WORKSPACE_ID, listOf("","","",""))
            }
            return HmUsages.Ref(PCommon.VC(WS_TYPE, RA_WORKSPACE_ID, -1, CAPTION, cl), key, vspec)
        }
    }

    // -----------------------------------------------------------------------------------------------
    fun load(rootPath: Path) {
        objectss.clear()
        rootPath.listDirectoryEntries().forEach { px ->
            val po = px.resolve("objects.json")
            if (Files.isRegularFile(po)) {
                val o = decodeObjectsFromString(po.readText())
                o.init(px)
                objectss.add(o)
            }
        }
    }

    fun navigation(sreq: String): String {
        val c = cnt++
        val pr = tracedir.resolve("${c}_navirequest.xml")
        pr.writeText(sreq)
        val navigationRequestInput = SimpleQuery.decodeNavigationFromString(sreq)

        val header = SimpleQuery.HeaderInfo(5, "type", "existenceFlag")
        val matrix = SimpleQuery.Matrix()
        var rez = SimpleQuery.QResult(header, SimpleQuery.TypeInfo(), matrix).encodeToString()
        val res2 = """<queryResult>
    <headerInfo xmlns="">
        <rows count="5" />
        <cols count="2" />
        <colDef>
            <def type="type" pos="0" />
            <def type="existenceFlag" pos="1" />
        </colDef>
    </headerInfo>
    <typeInfo xmlns="" />
    <matrix xmlns="">
        <r>
            <c>
                <type id="ifmtypedef" />
            </c>
            <c>
                <simple>
                    <bool>true</bool>
                </simple>
            </c>
        </r>
        <r>
            <c>
                <type id="ifmmessif" />
            </c>
            <c>
                <simple>
                    <bool>true</bool>
                </simple>
            </c>
        </r>
        <r>
            <c>
                <type id="ifmfaultm" />
            </c>
            <c>
                <simple>
                    <bool>true</bool>
                </simple>
            </c>
        </r>
        <r>
            <c>
                <type id="ifmmessage" />
            </c>
            <c>
                <simple>
                    <bool>true</bool>
                </simple>
            </c>
        </r>
        <r>
            <c>
                <type id="FOLDER" />
            </c>
            <c>
                <simple>
                    <bool>false</bool>
                </simple>
            </c>
        </r>
    </matrix>
</queryResult>"""
        return res2
    }

    fun handle(sreq: String): String {
        val c = cnt++
        val pr = tracedir.resolve("${c}_request.xml")
        pr.writeText(sreq)
        val req = SimpleQuery.decodeRequestFromString(sreq)
        var rez: String
        val cds = req.result.attrib.mapIndexed { i, s -> SimpleQuery.Def(s, i) }

        val singleEQ = SimpleQuery.RequestByNameNamespaceEQ.fromRequest(req)

        if (req.etypes.contains(MPI.ETypeID.workspace)) {
            println("handler ask workspace(s) $pr")
            val ks = listOf(SimpleQuery.KeyElem("WS_ID", 0), SimpleQuery.KeyElem("WS_ORDER", 1))
            val typei = SimpleQuery.TypeInfo(listOf(SimpleQuery.Type(MPI.ETypeID.workspace, ks)))
            if (req.condition == SimpleQuery.conditionWS_TYPE_S) {
                val header = SimpleQuery.HeaderInfo(objectss.size, cds.size, SimpleQuery.ColDef(cds))
                val matrix = SimpleQuery.Matrix(objectss.map { it.workspace.toR() })
                rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
            } else {
                val name = req.condition.complex!!.getSingle("NAME").value.simple.strg
                val version = req.condition.complex.getSingle("VERSION").value.simple.strg
                val wksps = objectss.filter { it.workspace.NAME == name && it.workspace.VERSION == version }

                val header = SimpleQuery.HeaderInfo(wksps.size, cds.size, SimpleQuery.ColDef(cds))
                val matrix = SimpleQuery.Matrix(objectss.map { it.workspace.toR() })
                rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
            }
        } else if (req.etypes.contains(MPI.ETypeID.namespace)) {
            println("handler ask namespace $pr")
            val ks = listOf(SimpleQuery.KeyElem("NAME", 0), SimpleQuery.KeyElem("NAMESPACE", 1))
            val typei = SimpleQuery.TypeInfo(MPI.ETypeID.namespace, "name")
            val swcvids = req.qc.swcListDef?.swcInfoList?.swc?.map { it.id } ?: listOf()
            val ox = mutableListOf<SimpleQuery.R>()
            swcvids.forEach { guid ->
                val wksp = objectss.filter { it.workspace.RA_WORKSPACE_ID == guid }
                require(wksp.size < 2, { "duplicates" })
                wksp.forEach { wk ->
                    ox.addAll(wk.namespacesToR())
                }
            }
            val header = SimpleQuery.HeaderInfo(ox.size, cds.size, SimpleQuery.ColDef(cds))
            val matrix = SimpleQuery.Matrix(ox)
            rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
        } else if (singleEQ != null) {
            println("handler get single $singleEQ $pr")
            val ks = listOf(SimpleQuery.KeyElem("NAME", 0), SimpleQuery.KeyElem("NAMESPACE", 1))
            val typei = SimpleQuery.TypeInfo(listOf(SimpleQuery.Type(singleEQ.entity, ks)))

            val ox = objectss.flatMap { it.findObjects(singleEQ) }
            val header = SimpleQuery.HeaderInfo(ox.size, cds.size, SimpleQuery.ColDef(cds))
            val matrix = SimpleQuery.Matrix(ox)

            rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
        } else {
            System.err.println("handler(UNIMPLEMENTED) $pr")
            TODO()
        }
        val pt = tracedir.resolve("${c}_response.xml")
        pt.writeText(rez)
        return rez
    }

    fun decodeObjectsFromString(s: String) = Json.decodeFromString<Objects>(s)

}