package karlutka.parsers.pi

import karlutka.models.MPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class SimpleQuery {
    // EQ, NOT_EQ, LIKE, NOT_LIKE, GE, LE, GT, BETWEEN, NOT_BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL
    enum class EOps { EQ }

    @Serializable
    @SerialName("generalQueryRequest")
    class QRequest(
        val types: Types,
        val qc: QC,
        val condition: Condition,
        val result: Result,
    ) {
        val etypes = types.type.map { it.id }
        fun encodeToString() = XML.encodeToString(this)
        //fun contains(t: MPI.ETypeID) = etypes.contains(t)
    }

    @Serializable
    @SerialName("queryResult")
    class QResult(
        val headerInfo: HeaderInfo,
        val typeInfo: TypeInfo,
        val matrix: Matrix,
        @XmlElement(true) val messages: String? = null
    ) {
        fun encodeToString() = XML.encodeToString(this)
        private fun toTable(): List<MutableMap<String, C>> {
            val lines = mutableListOf<MutableMap<String, C>>()
            val posTypeMapping = headerInfo.colDef.def.associate { Pair(it.pos, it.type) }    // 0:"", 1:RA_WORKSPACE_ID, 2:WS_NAME
            matrix.r.forEach { row ->
                val res = mutableMapOf<String, C>()
                row.c.forEachIndexed { cx, col ->
                    val cn = posTypeMapping[cx]
                    requireNotNull(cn)
                    if (cn.isNotBlank()) {
                        res[cn] = col
                    }
                }
                lines.add(res)
            }
            require(lines.size == headerInfo.rows.count) { "Must be ${headerInfo.rows.count} but found ${lines.size}" }
            return lines
        }

        fun toSwcv(): List<MPI.Swcv> {
            return toTable().map { x ->
                MPI.Swcv(
                    x["RA_WORKSPACE_ID"]!!.strvalue()!!,
                    x["NAME"]!!.strvalue(),
                    x["WS_NAME"]!!.strvalue(),
                    x["VENDOR"]!!.strvalue(),
                    x["VERSION"]!!.strvalue(),
                    null
                )   //TODO
//                    x["WS_TYPE"]!!.strvalue()!![0],
//                    x["ORIGINAL_LANGUAGE"]!!.strvalue()!!,
//                )
            }
        }

        // см. PI.namespaces
        fun toNamespace(swcv: List<MPI.Swcv>): List<MPI.Namespace> {
            val t = toTable().map { x ->
                val c = x["RA_NSP_STRING"]!!
                val text = ""                   // текстов нет
                val nsp = c.nsp!!
                val namespaceurl = nsp.key.elem[0]
                val oid = nsp.ref.key.oid
                require(nsp.key.typeID == "namespace")
                requireNotNull(nsp.ref)
                if (namespaceurl.isNotBlank()) {
                    // почему-то есть неймспейсы с пустыми значениями
                    val sw = swcv.find { it.guid == oid }
                    requireNotNull(sw) { "SWCV oid=$oid title=${nsp.ref.vc.caption} не найден для неймспейса '$namespaceurl'" }
                    MPI.Namespace(namespaceurl, sw, text)
                } else {
                    null
                }
            }
            return t.filterNotNull()
        }

        fun toList(swcv: List<MPI.Swcv>): List<MPI.HmiType> {
            val rez = mutableListOf<MPI.HmiType>()
            val posTypeMapping = headerInfo.colDef.def.associate { Pair(it.pos, it.type) }
            matrix.r.forEach { row ->
                lateinit var ref: HmUsages.Ref
                val att1 = mutableMapOf<String, String>()
                val att2 = mutableMapOf<String, String>()
                row.c.forEachIndexed { cx, col ->
                    val cn = posTypeMapping[cx]         //RA_XILINK, MODIFYDATE и прочие атрибуты
                    requireNotNull(cn)
                    require(cn.isNotBlank())
                    when (cn) {
                        "RA_WORKSPACE_ID" -> {
                            // это namespdecl у которого нет RA_XILINK. Собираем суррогатную ссылку.
                            val swcguid = col.wkID!!.id
                            ref = HmUsages.Ref(
                                PCommon.VC('?', null),
                                PCommon.Key("namespdecl", swcguid),
                                HmUsages.Ref.VSpec(4, swcguid, false)    //versionid := objectid := swcguid
                            )
                        }

                        "RA_XILINK" -> ref = col.qref!!.ref
                        "TEXT", "MODIFYUSER" -> att1[cn] = col.simple!!.strg!!
                        "FOLDERREF" -> att1[cn] = col.simple!!.bin!!
                        "MODIFYDATE" -> att1[cn] = col.simple!!.date!!
                        "EDITABLE", "ORIGINAL" -> requireNotNull(col.simple!!.bool) //пока эти атрибуты namespdecl никуда не пишем
                        // все прикладные атрибуты в другой словарь
                        else -> {
                            if (col.simple != null && col.simple.strg != null) {
                                att2[cn] = col.simple.strg
                            } else if (col.simple != null) {
                                error(cn)
                            }
                        }
                    } // смотрим какие атрибуты
                } // цикл по столбцам
                var swcref: MPI.Swcv? = null
                if (ref.vc.swcGuid != null) {
                    //TODO добавить SP в чтение SWCV
                    val sublist = swcv.filter { it.guid == ref.vc.swcGuid }
                    require(sublist.size == 1) { "Для SWCV ${ref.vc.swcGuid} более одного объекта" }
                    swcref = sublist[0]
                }
                val h = MPI.HmiType(
                    ref.key.typeID,
                    ref.key.oid!!,
                    ref.key.elem,
                    ref.vspec!!.id,
                    ref.vspec!!.deleted,
                    att1["TEXT"],
                    att1["FOLDERREF"],
                    att1["MODIFYUSER"],
                    att1["MODIFYDATE"],
                    att2,
                    swcref
                )
                rez.add(h)
            }
            require(rez.size == headerInfo.rows.count) { "Должно быть ${headerInfo.rows.count} строк но получено ${rez.size}" }
            return rez
        }
    }

    @Serializable
    @SerialName("navigationRequestInput")
    class NavigationRequestInput (
        val navigationRequest: NavigationRequest,
        val queryAttributes: QueryAttributes
    )

    @Serializable
    @SerialName("navigationRequest")
    class NavigationRequest(
        @XmlElement val navigationCursor: NavigationCursor,
        @XmlElement val existenceCheckOnly: Boolean
    )

    @Serializable
    @SerialName("navigationCursor")
    class NavigationCursor(
        @XmlElement val wkID: WkID,
        @XmlElement val type: Type,
        @XmlElement val namespaces: Namespaces,
        @XmlElement val types: Types,
    )

    @Serializable
    @SerialName("namespaces")
    class Namespaces(
        @XmlElement val namespace: List<Namespace>
    )

    @Serializable
    @SerialName("namespace")
    class Namespace(
        val name: String
    )

    @Serializable
    @SerialName("queryAttributes")
    class QueryAttributes(@XmlElement val attribute: List<String>)

    data class RequestByNameNamespaceEQ(val entity: MPI.ETypeID, val name: String, val namespace: String, val attrib: List<String>) {
        companion object {
            fun fromRequest(qr: QRequest): RequestByNameNamespaceEQ? {
                val e = qr.condition.complex?.elementary
                val eqs = e?.filter { it.single.op == EOps.EQ }?.associate { Pair(it.single.key, it.single.value.simple.strg) } ?: mapOf()
                val name = eqs["NAME"]
                val namespace = eqs["NAMESPACE"]
                if (qr.etypes.size == 1 && name != null && namespace != null)
                    return RequestByNameNamespaceEQ(qr.etypes[0], name, namespace, qr.result.attrib)
                else
                    return null
            }
        }
    }

    @Serializable
    @SerialName("types")
    class Types(
        val type: List<Type> = listOf(),
    )

    @Serializable
    @SerialName("type")
    class Type(
        @XmlElement(false)
        @SerialName("id")
        val id: MPI.ETypeID,
        val key: List<KeyElem> = listOf(),
    )

    @Serializable
    @SerialName("keyElem")
    class KeyElem(
        val name: String,
        val pos: Int,
    )

    @Serializable
    @SerialName("qc")
    class QC(
        @Serializable val qcType: Char = 'S',   //D-dir, S-rep
        @Serializable val delMode: Char = 'N',  //N-неудалённые, D-удалённые, B-both
        @XmlElement(true) @XmlSerialName("clCxt", "", "") var clCxt: PCommon.ClCxt,
        @XmlElement(true) var swcListDef: SwcListDef? = null,   //для /dir его нет
    )

    @Serializable
    @XmlSerialName("swcListDef", "", "")
    class SwcListDef(
        @Serializable val def: Char,
        @Serializable val swcInfoList: SwcInfoList? = null,
    )

    // ------------------------------------------------------------------------------------------

    @Serializable
    @XmlSerialName("condition", "", "")
    data class Condition(
        @XmlElement(true) val complex: Complex? = null,
        @XmlElement(true) val elementary: Elementary? = null,
    )

    @Serializable
    @XmlSerialName("complex", "", "")
    data class Complex(
        @XmlElement(true) val elementary: List<Elementary> = listOf(),
    ) {
        constructor(e: Elementary) : this(listOf(e))
        constructor(s: Single) : this(listOf(Elementary(s)))

        fun getSingle(key: String): Single {
            return elementary.find { it.single.key == key }!!.single
        }
    }

    @Serializable
    @SerialName("elementary")
    data class Elementary(
        @XmlElement(true) val single: Single,
    )

    @Serializable
    @SerialName("single")
    data class Single(
        @XmlElement(true) val key: String,
        // Value types:#BO#(Boolean),#DT#(Date),#GU#(Guid), #IN#(Integer),#XK#(XiKey),#XR#(XiRef),#AG#(Guid[])
        @XmlElement(true) val value: Val,
        @XmlElement(true) @XmlSerialName("op", "", "") val op: EOps,
    )

    @Serializable
    @XmlSerialName("val", "", "")
    data class Val(
        @XmlElement(true) val simple: Simple
    )

    @Serializable
    @XmlSerialName("simple", "", "")
    // Value types:#BO#(Boolean),#DT#(Date),#GU#(Guid), #IN#(Integer),#XK#(XiKey),#XR#(XiRef),#AG#(Guid[])
    data class Simple(
        @XmlElement(true) val strg: String? = null,
        @XmlElement(true) val int: Int? = null,
        @XmlElement(true) val bool: Boolean? = null,
        @XmlElement(true) val date: String? = null, //2022-01-12T10:19:12
        @XmlElement(true) val bin: String? = null,  //00000000000000000000000000000000
    )
//       {     constructor(a: String) : this(a, null, null) }

    @Serializable
    @XmlSerialName("result", "", "")
    class Result(
        @XmlElement(true) val attrib: List<String> = listOf(),
    ) {
        companion object {
            fun of(vararg names: String) = Result(names.asList())
        }
    }

    @Serializable
    @XmlSerialName("swcInfoList", "", "")
    class SwcInfoList(
        @XmlElement(true) val swc: List<SWC> = listOf(),
    ) {
        companion object {
            fun of(vararg swcv: String) = SwcInfoList(swcv.map { SWC(it, "-1", false) })
            fun of(swcv: List<String>) = SwcInfoList(swcv.map { SWC(it, "-1", false) })
        }
    }

    @Serializable
    @XmlSerialName("swc", "", "")
    class SWC(
        @Serializable val id: String,
        @Serializable val sp: String?,
        @Serializable val underL: Boolean,
    )

    @Serializable
    @XmlSerialName("headerInfo", "", "")
    class HeaderInfo(
        @XmlElement(true) @XmlSerialName("rows", "", "") val rows: Counted,
        @XmlElement(true) @XmlSerialName("cols", "", "") val cols: Counted,
        @XmlElement(true) val colDef: ColDef,
    ) {
        constructor(rows: Int, cols: Int, cd: ColDef) : this(Counted(rows), Counted(cols), cd)
        constructor(rows: Int, vararg cols: String) : this(Counted(rows), Counted(cols.size), ColDef(cols.mapIndexed { ix, s -> Def(s, ix) }))
        constructor(rows: Int, cols: List<String>) : this(Counted(rows), Counted(cols.size), ColDef(cols.mapIndexed { ix, s -> Def(s, ix) }))
    }


    @Serializable
    @XmlSerialName("qref", "", "")
    class Qref(
        @Serializable val isMod: Boolean,
        @Serializable val isInUnderL: Boolean,
        @XmlElement(true) val ref: HmUsages.Ref,
    )

    @Serializable
    @XmlSerialName("nsp", "", "")
    class Nsp(
        val isUL: Boolean,
        @XmlElement(true) @XmlSerialName("key", "", "") val key: PCommon.Key,
        @XmlElement(true) @XmlSerialName("ref", "", "") val ref: HmUsages.Ref,
    )

    @Serializable
    @XmlSerialName("wkID", "", "")
    class WkID(
        val id: String,
        val order: Int?,    //в navigation нет
    )

    @Serializable
    @XmlSerialName("c", "", "")
    class C(
        @XmlElement(true) val wkID: WkID? = null,
        @XmlElement(true) val simple: Simple? = null,
        @XmlElement(true) val qref: Qref? = null,
        @XmlElement(true) val nsp: Nsp? = null,
        @XmlElement(true)
        @XmlSerialName("array", "", "")
        val array: List<QArray> = listOf()
    ) {
        constructor(s: String?) : this(null, Simple(s))
        constructor(c: Char) : this(null, Simple(c.toString()))

        fun strvalue(): String? {
            return if (qref != null) {
                "qref=" + qref.ref.key.typeID
            } else if (nsp != null) {
                nsp.key.elem[0]      //неймспейсы
            } else wkID?.id ?: simple?.strg
        }

        override fun toString() = "C(${strvalue()})"

    }

    @Serializable
    @XmlSerialName("array", "", "")
    class QArray(
        @XmlElement(true) val simple: Simple? = null,
        @XmlElement(true) val ref: HmUsages.Ref? = null,
    )

    @Serializable
    @XmlSerialName("r", "", "")
    class R(
        @XmlElement(true) val c: List<C> = listOf(),
    )

    @Serializable
    @XmlSerialName("matrix", "", "")
    class Matrix(
        @XmlElement(true) val r: List<R> = listOf(),
    )

    @Serializable
    @XmlSerialName("typeInfo", "", "")
    class TypeInfo(
        @XmlSerialName("type", "", "") val type: List<Type> = listOf(),
    ) {
        // Один тип и перечень ключа
        constructor(e: MPI.ETypeID, vararg keys: String) : this(listOf(Type(e, keys.mapIndexed { ix, s -> KeyElem(s, ix) })))
    }

    @Serializable
    @XmlSerialName("def", "", "")
    class Def(
        val type: String,
        val pos: Int,
    )

    @Serializable
    @XmlSerialName("colDef", "", "")
    class ColDef(
        @XmlElement(true) val def: List<Def> = listOf(),
    )

    @Serializable
    class Counted(
        val count: Int,
    )

    // --------------------------------------------------------------------------------------------
    companion object {
        val conditionWS_TYPE_S = Condition(Complex(Single("WS_TYPE", Val(Simple("S")), EOps.EQ)))
        fun decodeRequestFromReader(x: XmlReader): QRequest {
            return XML.Companion.decodeFromReader(x)
        }

        fun decodeRequestFromString(s: String): QRequest {
            return XML.Companion.decodeFromString(s)
        }

        fun decodeResultFromReader(x: XmlReader): QResult {
            return XML.Companion.decodeFromReader(x)
        }

        fun decodeNavigationFromReader(x: XmlReader) : NavigationRequestInput {
            return XML.Companion.decodeFromReader(x)
        }
        fun decodeNavigationFromString(s: String) : NavigationRequestInput {
            return XML.Companion.decodeFromString(s)
        }
    }

}


//        companion object {
//            fun ofArg(lst: List<String>, cond: Condition?, vararg result: String): GeneralQueryRequest {
//                val lst2 = lst.map { Type(it) }
//                val res = Result(result.toList())
//
//                return GeneralQueryRequest(
//                    Types(lst2), QC("S", "N", PCommon.ClCxt("S", "U"), SwcListDef("A")), cond ?: Condition(), res
//                )
//            }

//            fun ofType(type: String, cond: Condition?, vararg result: String): GeneralQueryRequest {
//                return ofArg(listOf(type), cond, *result)
//            }

//            @Deprecated("удоли")
//            private fun elementary(key: String, op: String, c: Simple): Condition {
//                return Condition(null, Elementary(Single(key, Val(c), op)))
//            }

//            fun namespaces(swc: List<String>): Request {
//                val swcinfolist = SwcInfoList(swc.map { x -> SWC(x, "-1", false) })
//                return Request(
//                    Types(listOf(Type("namespace"))),
//                    QC('S', 'N', PCommon.ClCxt('S', "User"), SwcListDef('G', swcinfolist)),
//                    Condition(),
//                    RResult(listOf("RA_NSP_STRING", "TEXT"))
//                )
//            }
//        }
