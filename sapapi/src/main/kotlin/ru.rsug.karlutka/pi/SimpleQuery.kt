package ru.rsug.karlutka.pi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class SimpleQuery {
    @Suppress("unused")
    enum class EOps { EQ, NOT_EQ, LIKE, NOT_LIKE, GE, LE, GT, BETWEEN, NOT_BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL }

    @Suppress("unused")
    enum class EResult {
        // --- репозитори
        RA_NSP_STRING,
        RA_WORKSPACE_ID,
        WS_NAME,
        VENDOR,
        VERSION,

        // --- директори
        // Икохи
        RA_XILINK,
        OBJECTID,
        VERSIONID,
        TEXT,
        MODIFYUSER,
        MODIFYDATE,
        FOLDERREF,
        PARAMETER,
        OWNER,
        Q_TEXT_LABEL_LONG,
        Q_TEXT_LABEL_SHORT,
        Q_TEXT_SHORT,
        RA_CHECK_EXISTENCE_OF_TYPE,         // проверяет, существует ли скажем икоха в системе
        RA_CHECK_EXISTENCE_OF_TYPEnot,
        RA_LINK_LIST,                       // список ссылок например с ICo
        RA_LINK_LIST_ROLE,
        RA_LINK_LIST_ROLE_POS,
        RA_LINK_POSITION,
        RA_LINK_RESOLVEMODE,
        RA_LINK_ROLE,
        RA_LINK_TARGET_OID,
        RA_LINK_TARGET_REFERENCE,
        RA_LINK_TARGET_TYPE,
        RA_TEXT_LANGUAGE_LONG,
        RA_TEXT_LANGUAGE_SHORT,
        RA_TEXT_LONG,
        TOSRVC,
        TOPARTY,
        VALIDATIONMODE,
        FROMSRVC,
        FROMPARTY,
        FROMACTION,
        FROMACTIONNS,
        USEGLOBAL,
        VIRUSSCANMODE,

        // Сервисы
        ISNWBPM,
        CHARGEABLE,
        SERVICEIDNTFR,
        SERVICESCHEMA,
        SERVICETYPE,

        // Партии
        PARTYAGENCY,
        PARTYIDNTFR,
        PARTYSCHEMA,

        // Сценарии
        DIRVIEW,
        DIR_VIEW_NAME,

        // VMG
        AGENCY,
        GROUPNAME,
        SCHEME,
        VMVALUE,

        // Каналы
        ENGINENAME,
        ENGINETYPE,
        CHANNEL,
        SERVICE,
        DIRECTION,
        MSGPROT,
        MSGPROTVERS,
        MODULENAME,
        MODULETYPE,
        PARTY,
        TRNSPROTVERS,
        TRNSPROT,

        // документация
        NAME,
        NAMESPACE,

        // алерт рулы
        BLOCKALERTTIME,
        RULETYPE,
        CONSUMER,
        RUNTIMESTATE,
        ERRORLABEL,
        RUNTIME,
        SEVERITY,
        SUPPRESSTIME,
        PAYLOAD,

        // navigationRequestInput
        type,
        existenceFlag
    }

    @Serializable
    @SerialName("generalQueryRequest")
    class QueryRequest(
        val types: Types,
        val qc: QC,
        val condition: Condition,
        val result: Result,
    ) {
        val etypes = types.type.map { it.id }
        fun encodeToString() = XML.encodeToString(this)
    }

    @Serializable
    @SerialName("queryResult")
    class QueryResult(
        val headerInfo: HeaderInfo,
        val typeInfo: TypeInfo,
        val matrix: Matrix,
        @XmlElement(true) val messages: String? = null,
    ) {
        fun encodeToString() = XML.encodeToString(this)
    }

    @Serializable
    @SerialName("navigationRequestInput")
    class NavigationRequestInput(
        val navigationRequest: NavigationRequest,
        val queryAttributes: QueryAttributes,
    )

    @Serializable
    @SerialName("navigationRequest")
    data class NavigationRequest(
        @XmlElement val navigationCursor: NavigationCursor,
        @XmlElement val existenceCheckOnly: Boolean,
    )

    @Serializable
    @SerialName("navigationCursor")
    data class NavigationCursor(
        @XmlElement val wkID: WkID,
        @XmlElement val type: Type,
        @XmlElement val namespaces: Namespaces,
        @XmlElement val types: Types,
    )

    @Serializable
    @SerialName("namespaces")
    class Namespaces(
        @XmlElement val namespace: List<Namespace>,
    )

    @Serializable
    @SerialName("namespace")
    class Namespace(
        val name: String,
    )

    @Serializable
    @SerialName("queryAttributes")
    class QueryAttributes(
        @XmlElement
        @XmlSerialName("attribute", "", "")
        val attribute: List<EResult>,
    )

    class RequestByNameNamespaceEQ(
        val entity: MPI.ETypeID, val name: String, val namespace: String, val attrib: List<EResult>,
    ) {
        companion object {
            fun fromRequest(qr: QueryRequest): RequestByNameNamespaceEQ? {
                val e = qr.condition.complex?.elementary
                val eqs = e?.filter { it.single.op == EOps.EQ }?.associate { Pair(it.single.key, it.single.value.simple.strg) } ?: mapOf()
                val name = eqs["NAME"]
                val namespace = eqs["NAMESPACE"]
                return if (qr.etypes.size == 1 && name != null && namespace != null)
                    RequestByNameNamespaceEQ(qr.etypes[0], name, namespace, qr.result.attrib)
                else
                    null
            }
        }
    }

    @Serializable
    @SerialName("types")
    class Types(
        val type: List<Type> = listOf(),
    ) {
        constructor(vararg ids: MPI.ETypeID) : this(ids.toList().map { Type(it) })
    }

    @Serializable
    @SerialName("type")
    class Type(
        @XmlElement(false) @SerialName("id") val id: MPI.ETypeID,
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
//        constructor(e: Elementary) : this(listOf(e))
//        constructor(s: Single) : this(listOf(Elementary(s)))

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
        @XmlElement(true) val value: Val,
        @XmlElement(true) @XmlSerialName("op", "", "") val op: EOps,
    )

    @Serializable
    @XmlSerialName("val", "", "")
    data class Val(
        @XmlElement(true) val simple: Simple,
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

    @Serializable
    @XmlSerialName("result", "", "")
    class Result(
        @XmlElement(true)
        @XmlSerialName("attrib", "", "")
        val attrib: List<EResult> = listOf(),
    ) {
        constructor(vararg names: EResult) : this(names.asList())
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
        //constructor(rows: Int, cols: Int, cd: ColDef) : this(Counted(rows), Counted(cols), cd)
        //constructor(rows: Int, vararg cols: String) : this(Counted(rows), Counted(cols.size), ColDef(cols.mapIndexed { ix, s -> Def(s, ix) }))
        //constructor(rows: Int, cols: List<String>) : this(Counted(rows), Counted(cols.size), ColDef(cols.mapIndexed { ix, s -> Def(s, ix) }))
    }


    @Serializable
    @XmlSerialName("qref", "", "")
    class Qref(
        @Serializable val isMod: Boolean,
        @Serializable val isInUnderL: Boolean,
        @XmlElement(true) val ref: HmiUsages.Ref,
    )

    @Serializable
    @XmlSerialName("nsp", "", "")
    class Nsp(
        val isUL: Boolean,
        @XmlElement(true) @XmlSerialName("key", "", "") val key: PCommon.Key,
        @XmlElement(true) @XmlSerialName("ref", "", "") val ref: HmiUsages.Ref,
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
        val array: List<QArray> = listOf(),
        @XmlElement(true) val type: Type? = null,
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
        @XmlElement(true) val ref: HmiUsages.Ref? = null,
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
        //constructor(e: MPI.ETypeID, vararg keys: String) : this(listOf(Type(e, keys.mapIndexed { ix, s -> KeyElem(s, ix) })))
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

    companion object {
        // будет использовано в FESR или утащить туда
        val conditionWS_TYPE_S = Condition(Complex(listOf(Elementary(Single("WS_TYPE", Val(Simple("S")), EOps.EQ)))))

        /**
         * Запрос в HMI для /dir/support/SimpleQuery
         * @param types опрашиваемые объекты
         * @param results список атрибутов
         * @param condition условие по которому выбор
         */
        fun queryRequestDir(types: List<MPI.ETypeID>, results: List<EResult>, condition: Condition = Condition()): QueryRequest {
            // вытаскиваем объекты types из 'D'irectory 'N'еудалённые 'A'ктивные, только OID и TEXT
            val qc = QC('D', 'N', PCommon.ClCxt('A'))
            return QueryRequest(Types(types.map { Type(it) }), qc, condition, Result(results))
        }

        /**
         * Запрос в HMI для /dir/support/SimpleQuery
         * @param what запрашиваемый тип или результат или условие
         */
        fun queryRequestDirV(vararg what: Any): QueryRequest {
            val types = what.filterIsInstance<MPI.ETypeID>()
            val results = what.filterIsInstance<EResult>()
            val condition: Condition = what.find { it is Condition } as Condition? ?: Condition()
            return queryRequestDir(types, results, condition)
        }

        /**
         * Запрос в HMI для /rep/support/SimpleQuery без SWCV
         */
        fun queryRequestRep(types: List<MPI.ETypeID>, results: List<EResult>, condition: Condition = Condition()): QueryRequest {
            // <qc xmlns="" qcType="S" delMode="N" useSyncT="true" clCtxOnly="false">
            //		<clCxt consider="A" user="?"/>
            //		<swcListDef def="W"/>
            //	</qc>
            val qc = QC('S', 'N', PCommon.ClCxt('A'), SwcListDef('W'))
            return QueryRequest(Types(types.map { Type(it) }), qc, condition, Result(results))
        }

        /**
         * Запрос в HMI для /rep/support/SimpleQuery с SWCV
         */
        fun queryRequestRep(wkIDs: List<String>, types: List<MPI.ETypeID>, results: List<EResult>, condition: Condition = Condition()): QueryRequest {
            // 	<qc qcType="S" delMode="N">
            //		<clCxt consider="A" user="?"/>
            //		<swcListDef def="G">
            //			<swcInfoList>
            //				<swc id="34ee13000ded11eb95bcfc3eac130d0e" underL="true"/>
            //				<swc id="55196dd06c7711eb98b2e89fac130d0e" underL="true"/>
            //				<swc id="b82055b0897311e6b783c9af0aa2b0df" underL="true"/>
            //			</swcInfoList>
            //		</swcListDef>
            //	</qc>
            val qc = QC('S', 'N', PCommon.ClCxt('A'), SwcListDef('G', SwcInfoList(wkIDs.map{SWC(it, null, false)})))
            return QueryRequest(Types(types.map { Type(it) }), qc, condition, Result(results))
        }


        fun decodeQueryRequestFromReader(x: XmlReader): QueryRequest {
            return XML.Companion.decodeFromReader(x)
        }

        fun decodeQueryRequestFromString(s: String): QueryRequest {
            return XML.Companion.decodeFromString(s)
        }

        fun decodeQueryResultFromString(s: String): QueryResult {
            return XML.Companion.decodeFromString(s)
        }

        fun decodeQueryResultFromReader(x: XmlReader): QueryResult {
            return XML.Companion.decodeFromReader(x)
        }

        fun decodeNavigationRequestInputFromReader(x: XmlReader): NavigationRequestInput {
            return XML.Companion.decodeFromReader(x)
        }

        fun decodeNavigationRequestInputFromString(s: String): NavigationRequestInput {
            return XML.Companion.decodeFromString(s)
        }
    }
}
