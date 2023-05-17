package karlutka.server

import karlutka.models.MPI
import karlutka.parsers.pi.HmiUsages
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

        fun findObjects(nrq: SimpleQuery.NavigationRequestInput): SimpleQuery.QResult {
            val askedEntity = nrq.navigationRequest.navigationCursor.type.id                // namespace, etc
            val askedTypes = nrq.navigationRequest.navigationCursor.types.type              // много
            val onNamespaces = nrq.navigationRequest.navigationCursor.namespaces.namespace  // вживую 1..1 но может быть 1..N

            val relst = mutableListOf<SimpleQuery.R>()
            onNamespaces.forEach { ns ->
                val namespaceFolder = nsToPath[ns.name] //в этой папке ищем заданные объекты, по расширению
                requireNotNull(namespaceFolder)
                askedTypes.forEach { at ->
                    val found = namespaceFolder.listDirectoryEntries("*.${at.id}")
                    if (found.isNotEmpty()) {
                        relst.add(
                            SimpleQuery.R(
                                listOf(
                                    SimpleQuery.C(null, null, null, null, listOf(), at),
                                    SimpleQuery.C(null, SimpleQuery.Simple(null, null, true))
                                )
                            )
                        )
                    }
                }

            }

            val matrix = SimpleQuery.Matrix(relst)
            val header = SimpleQuery.HeaderInfo(matrix.r.size, "type", "existenceFlag")
            if (nrq.navigationRequest.existenceCheckOnly) {
                // На входе перечень неймспейсов с обычно одним элементом и перечень типов
                return SimpleQuery.QResult(header, SimpleQuery.TypeInfo(), matrix)
            } else {
                System.err.println("(160) navigation received request //TODO : ${nrq.navigationRequest}")
                TODO()
            }
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

        fun toRef(x: XiObj?, withVersion: Boolean): HmiUsages.Ref {
            val cl = PCommon.ClCxt('A')
            val vspec: HmiUsages.Ref.VSpec? = if (x != null && withVersion)
                HmiUsages.Ref.VSpec(4, x.idInfo.VID!!, false)
            else
                null
            val key = if (x != null)
                x.idInfo.key
            else {
                // для 'namespace'
                PCommon.Key("swc", RA_WORKSPACE_ID, listOf("", "", "", ""))
            }
            return HmiUsages.Ref(PCommon.VC(WS_TYPE, RA_WORKSPACE_ID, -1, CAPTION, cl), key, vspec)
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

    fun navigation(method: String, sreq: String): String {
        require(method in listOf("naviquery"))
        val c = cnt++
        val pr = tracedir.resolve("${c}_navirequest.xml")
        val pt = tracedir.resolve("${c}_naviresponse.xml")
        pr.writeText(sreq)
        println("navigation($method) $pr->$pt")
        val nrq = SimpleQuery.decodeNavigationFromString(sreq)
        val wkGUID = nrq.navigationRequest.navigationCursor.wkID.id
        val sq = objectss.find { it.workspace.RA_WORKSPACE_ID == wkGUID }?.findObjects(nrq)
        val rez = sq!!.encodeToString()
        pt.writeText(rez)
        return rez
    }

    fun query(sreq: String): String {
        val c = cnt++
        val pr = tracedir.resolve("${c}_request.xml")
        val pt = tracedir.resolve("${c}_response.xml")
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
                val wksps = objectss.filter { it.workspace.WS_TYPE=='S' }
                val header = SimpleQuery.HeaderInfo(wksps.size, cds.size, SimpleQuery.ColDef(cds))
                val matrix = SimpleQuery.Matrix(wksps.map { it.workspace.toR() })
                rez = SimpleQuery.QResult(header, typei, matrix).encodeToString()
            } else {
                val name = req.condition.complex!!.getSingle("NAME").value.simple.strg
                val version = req.condition.complex.getSingle("VERSION").value.simple.strg
                val wstype = req.condition.complex.getSingle("WS_TYPE").value.simple.strg
                val wksps =
                    objectss.filter { it.workspace.NAME == name && it.workspace.VERSION == version && it.workspace.WS_TYPE.toString() == wstype }

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
        pt.writeText(rez)
        return rez
    }

    fun decodeObjectsFromString(s: String) = Json.decodeFromString<Objects>(s)

}