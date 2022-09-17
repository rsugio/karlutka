package karlutka.clients

import com.sap.conn.jco.JCoDestination
import com.sap.conn.jco.JCoDestinationManager
import com.sap.conn.jco.JCoRepository
import com.sap.conn.jco.JCoTable
import com.sap.conn.jco.ext.DestinationDataEventListener
import com.sap.conn.jco.ext.DestinationDataProvider
import com.sap.conn.jco.ext.Environment
import karlutka.models.MTarget
import karlutka.util.KfTarget
import java.time.LocalDateTime
import java.util.*

/*
PID: SXMB_GET_PIPELINES
Нам нужны лишь RECEIVER, SENDER и подтверждения: RECEIVER_BACK, SENDER_BACK
 */

class AbapJCo(override val konfig: KfTarget) : MTarget {
    private val runtime: JCoDestination
    private val repository: JCoRepository

    init {
        runtime = JCoDestinationManager.getDestination(konfig.sid)
        repository = runtime.repository
    }

    object ZDestinationDataProvider : DestinationDataProvider {
        val jcoClients: MutableMap<String, Properties> = mutableMapOf()

        init {
            Environment.registerDestinationDataProvider(this)
        }

        override fun getDestinationProperties(sid: String): Properties {
            return jcoClients[sid]!!
        }

        override fun supportsEvents() = false //TODO чекнуть что за штука
        override fun setDestinationDataEventListener(p0: DestinationDataEventListener?) {
            TODO("Not yet implemented")
        }
    }

//    fun getSID() = repository.name
//    fun getMANDT() = runtime.client

    fun STFC_CONNECTION(request: String = "русский привет"): String {
        val STFC_CONNECTION = repository.getFunction("STFC_CONNECTION")
        STFC_CONNECTION.importParameterList.setValue("REQUTEXT", request)
        STFC_CONNECTION.execute(runtime)
        val resp = STFC_CONNECTION.exportParameterList.getString("ECHOTEXT")
        require(request == resp, { "Предположительно проверка юникода с ошибкой: ожидалось '$request', получено '$resp'" })
        val info = STFC_CONNECTION.exportParameterList.getString("RESPTEXT")
        return info
    }

    // фильтр требует либо дату либо MessageID
    fun SXMB_GET_MESSAGE_LIST(
        filter: Any,
        messageCount: Int = 200,
    ) {
        TODO()
    }

    fun SXMB_GET_XI_MESSAGE(
        msgid: String,
        pid: String,
        archive: Boolean? = null,
        version: Int? = null,
        client: String? = null,
    ): ByteArray {
        val SXMB_GET_XI_MESSAGE = repository.getFunction("SXMB_GET_XI_MESSAGE")
        val IM_MSGKEY = SXMB_GET_XI_MESSAGE.importParameterList.getStructure("IM_MSGKEY")
        IM_MSGKEY.setValue("MSGID", msgid)
        IM_MSGKEY.setValue("PID", pid.toString())
        if (archive != null) {
            SXMB_GET_XI_MESSAGE.importParameterList.setValue("IM_ARCHIVE", if (archive) "X" else "")
        }
        if (version != null) {
            SXMB_GET_XI_MESSAGE.importParameterList.setValue("IM_VERSION", version.toString())
        }
        if (client != null) {
            SXMB_GET_XI_MESSAGE.importParameterList.setValue("IM_CLIENT", client.toString())
        }
        SXMB_GET_XI_MESSAGE.execute(runtime)
        val rez = SXMB_GET_XI_MESSAGE.exportParameterList.getByteArray("EX_MSG_BYTES")
        return rez
    }

    fun SXMB_GET_MESSAGE_DATA(vararg s: String) = SXMB_GET_MESSAGE_DATA(s.toMutableList())
    fun SXMB_GET_MESSAGE_DATA(
        msgs: MutableList<String> = mutableListOf(),
        pids: MutableList<String> = mutableListOf(), //SENDER, RECEIVER, хз что ещё
    ): JCoTable {
        val SXMB_GET_MESSAGE_DATA = repository.getFunction("SXMB_GET_MESSAGE_DATA")
        val IM_MESSAGE_LIST = SXMB_GET_MESSAGE_DATA.importParameterList.getTable("IM_MESSAGE_LIST")
        IM_MESSAGE_LIST.appendRows(msgs.size)
        IM_MESSAGE_LIST.firstRow()
        msgs.forEach { x -> IM_MESSAGE_LIST.setValue(0, x); IM_MESSAGE_LIST.nextRow() }

        val IM_PIPELINE_LIST = SXMB_GET_MESSAGE_DATA.importParameterList.getTable("IM_PIPELINE_LIST")
        IM_PIPELINE_LIST.appendRows(pids.size)
        IM_PIPELINE_LIST.firstRow()
        pids.forEach { x -> IM_PIPELINE_LIST.setValue(0, x); IM_PIPELINE_LIST.nextRow() }

        SXMB_GET_MESSAGE_DATA.execute(runtime)
        val EX_MESSAGE_DATA_LIST = SXMB_GET_MESSAGE_DATA.exportParameterList.getTable("EX_MESSAGE_DATA_LIST")
        return EX_MESSAGE_DATA_LIST
    }

    // можно сделать при желании - аналог IDOC_DATE_TIME_GET
    fun IDX_TRACING(
        selection_date: String = "00000000",    // абап дата yyyyMMdd необязательна
        source_system: String = "ZCLIENT",     // логическая система отправителя (EDI_DC40-SNDPRN)
        sourceIdocNrs: MutableList<String> = mutableListOf(),   // номера айдоков в системе-отправителе
    ) {
        // под капотом - IDOC_DATE_TIME_GET
        val IDX_TRACING = repository.getFunction("IDX_TRACING")
        TODO()
    }

    fun IDOC_DATE_TIME_GET(
        selection_date: String = "00000000",    // абап дата yyyyMMdd необязательна
        source_system: String = "0M2AS12R",     // логическая система отправителя (EDI_DC40-SNDPRN)
        sourceIdocNrs: MutableList<String> = mutableListOf(),   // номера айдоков в системе-отправителе
    ) {
        val IDOC_DATE_TIME_GET = repository.getFunction("IDOC_DATE_TIME_GET")
        IDOC_DATE_TIME_GET.importParameterList.setValue("SELECTION_DATE", selection_date)
        IDOC_DATE_TIME_GET.importParameterList.setValue("SOURCE_SYSTEM", source_system)
        val T_IDOCINFO = IDOC_DATE_TIME_GET.tableParameterList.getTable("T_IDOCINFO")
        T_IDOCINFO.appendRows(sourceIdocNrs.size)
        T_IDOCINFO.firstRow()
        sourceIdocNrs.forEach {
            T_IDOCINFO.setValue("DOCNUM_S", it)
            T_IDOCINFO.nextRow()
        }
        IDOC_DATE_TIME_GET.execute(runtime)
        T_IDOCINFO.firstRow()
        do {
            val DOCNUM_S = T_IDOCINFO.getString("DOCNUM_S")
            val CREDAT_R = T_IDOCINFO.getString("CREDAT_R")
            val CRETIM_R = T_IDOCINFO.getString("CRETIM_R")
            val DOCNUM_R = T_IDOCINFO.getString("DOCNUM_R")
            val STATUS_R = T_IDOCINFO.getString("STATUS_R")
            println("$DOCNUM_S\t$DOCNUM_R\t$CREDAT_R $CRETIM_R\t$STATUS_R")
        } while (T_IDOCINFO.nextRow())
    }

    fun RFC_READ_TABLE_EDIDS(docnum: String) {
        val RFC_READ_TABLE = repository.getFunction("RFC_READ_TABLE")
        RFC_READ_TABLE.importParameterList.setValue("QUERY_TABLE", "EDIDS")
        RFC_READ_TABLE.importParameterList.setValue("DELIMITER", "\t")
        val options = RFC_READ_TABLE.tableParameterList.getTable("OPTIONS")
        options.appendRow()
        options.firstRow()
        options.setValue(0, "DOCNUM eq '$docnum'")
        val fields = RFC_READ_TABLE.tableParameterList.getTable("FIELDS")
        fields.appendRows(12)
        fields.firstRow()
        fields.setValue("FIELDNAME", "LOGDAT"); fields.nextRow()
        fields.setValue("FIELDNAME", "LOGTIM"); fields.nextRow()
        fields.setValue("FIELDNAME", "COUNTR"); fields.nextRow()
        fields.setValue("FIELDNAME", "STATUS"); fields.nextRow()
        fields.setValue("FIELDNAME", "STATXT"); fields.nextRow()
        fields.setValue("FIELDNAME", "STAPA1"); fields.nextRow()
        fields.setValue("FIELDNAME", "STAPA2"); fields.nextRow()
        fields.setValue("FIELDNAME", "STAPA3"); fields.nextRow()
        fields.setValue("FIELDNAME", "STAPA4"); fields.nextRow()
        fields.setValue("FIELDNAME", "STATYP"); fields.nextRow()
        fields.setValue("FIELDNAME", "STAMID"); fields.nextRow()
        fields.setValue("FIELDNAME", "STAMNO"); fields.nextRow()
        RFC_READ_TABLE.execute(runtime)
        val data = RFC_READ_TABLE.tableParameterList.getTable("DATA")
        data.firstRow()
        do {
            val s = data.getString(0)
            val a = s.split("\t").map { it.trim() }
            require(a.size == 12)
            println(a)
        } while (data.nextRow())
    }

    fun SPRX_GET_SPROXDAT(): String {
        val SPRX_GET_SPROXDAT = repository.getFunction("SPRX_GET_SPROXDAT")
        val obj = SPRX_GET_SPROXDAT.importParameterList.getTable("OBJECTS")
        obj.appendRows(1)
        obj.setValue(0, "CLAS")
        obj.setValue(1, "ZCO_CO_SI_EXPENSES_COST_CENTRE")
        SPRX_GET_SPROXDAT.execute(runtime)
        val data = SPRX_GET_SPROXDAT.exportParameterList.getTable("SPROXDAT")
        data.firstRow()
        do {
            val type4 = data.getString("OBJECT")    //CLAS
            val obj_name = data.getString("OBJ_NAME")
            val id = data.getString("ID")
            val ifr_type = data.getString("IFR_TYPE")   //portType, operation, output
            val ifr_name = data.getString("IFR_NAME")   //portType, operation, output
        } while (data.nextRow())
        return ""
    }

    fun readtable_SXMSINTERFACE() {
        val RFC_READ_TABLE = repository.getFunction("RFC_READ_TABLE")
        RFC_READ_TABLE.importParameterList.setValue("QUERY_TABLE", "SXMSINTERFACE")
        RFC_READ_TABLE.importParameterList.setValue("DELIMITER", "\t")
        val fields = RFC_READ_TABLE.tableParameterList.getTable("FIELDS")
        fields.appendRows(13)
        fields.firstRow()
        fields.setValue("FIELDNAME", "OBJECT"); fields.nextRow()
        fields.setValue("FIELDNAME", "OBJ_NAME"); fields.nextRow()
        fields.setValue("FIELDNAME", "OBJECT1"); fields.nextRow()
        fields.setValue("FIELDNAME", "OBJ_NAME1"); fields.nextRow()
        fields.setValue("FIELDNAME", "IMPL_CLASS"); fields.nextRow()
        fields.setValue("FIELDNAME", "IFR_TYPE"); fields.nextRow()
        fields.setValue("FIELDNAME", "IFR_NSPCE"); fields.nextRow()
        fields.setValue("FIELDNAME", "IFR_INTF"); fields.nextRow()
        fields.setValue("FIELDNAME", "IFR_OPERATION"); fields.nextRow()
        fields.setValue("FIELDNAME", "IFR_GNSPCE"); fields.nextRow()
        fields.setValue("FIELDNAME", "GEN_VERS"); fields.nextRow()
        fields.setValue("FIELDNAME", "IFR_IDEMPOTENT"); fields.nextRow()
        fields.setValue("FIELDNAME", "CHANGED_ON"); fields.nextRow()
        RFC_READ_TABLE.execute(runtime)
        val data = RFC_READ_TABLE.tableParameterList.getTable("DATA")
        data.firstRow()
        do {
            val s = data.getString(0)
            val a = s.split("\t").map { it.trim() }
            require(a.size == 13)
            println(a)
        } while (data.nextRow())
    }



    /*
    0000000118760717
        Ещё RFC FM:
            IDX_GET_IDOC - поиск айдоков по атрибутам. Бесполезный.
            IDX_GET_GUID - фигня
            IDX_EDISDEF_ENTRIES_READ - чтение структуры айдока на уровне сегментов
            IDX_META_SYNTAX_READ - аналогично IDX_EDISDEF_ENTRIES_READ но с полями
            IDX5_CALL_VIA_RFC_DOCNUM - показывает айдок в сапгуе, прикольный
            GET_STATUS_FROM_IDOCNR - возврат последнего статуса (51, 53 и тд), полезный
            IDOC_RECORD_READ - какие-то статусы, неясно
            IDOC_CONTROL_DATA_CHECK - проверка профиля партнёра, круто

            INBOUND_IDOCS_FOR_TID - похоже, возвращает номер айдока по TID вида XI0PtU4woyItYtN8V{iz1tqG
            OUTBOUND_IDOCS_FOR_TID

            EDI_DOCUMENT_OPEN_FOR_READ - открыть айдок для чтения, достаточно номера
            EDI_DOCUMENT_CLOSE_READ
            EDI_DOCUMENT_READ_ALL_STATUS - читает статусы ОТКРЫТЫХ айдоков
            EDI_DOCUMENT_READ_LAST_STATUS - последний статус ОТКРЫТОГО айдока
            EDI_SEGMENTS_GET_ALL - чтение сегментов (?) ОТКРЫТОГО айдока

            EDM01_IDOC_MESSAGE_READ - локальный, чтение статусов
            EDM01_GET_STATUS_HISTORY - локальный, для текстов
            EDM01_SELECT_EDIDS - локальный, чтение статусов

       IV:  /SDF/IVIS_DCOLL_UPD_AND_RMV - для сбора данных
     */

    //TODO
    fun conv_yyyyMMddHHmmss_SSSSSSS(s: String = "20220803092951.3783400"): LocalDateTime {
        //TODO ЗАТЫЧКА
        return LocalDateTime.MIN
    }
}