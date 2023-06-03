package ru.rsug.karlutka.pi

class MPI {
    enum class DIRECTION { INBOUND, OUTBOUND }
    enum class ETypeID {
        workspace,
        namespace,
        namespdecl,
        AdapterMetaData,
        ChannelTemplate,
        DOCU,
        FOLDER,
        FUNC_LIB,
        MAPPING,
        MAP_TEMPLATE,
        MAP_FILE_NAME,      // встречалось пока лишь в ссылках
        MAP_ARCHIVE_PRG,    // встречалось пока лишь в ссылках
        MAP_IDENTY_PROG,    // встречалось пока лишь в ссылках
        MAP_IMP_XSD_XML,    // встречалось пока только в ссылках
        RepBProcess,        // шлак
        TRAFO_JAR,
        XI_TRAFO,
        ariscommonfile,     // шлак
        arisfilter,         // шлак
        arisfssheet,        // шлак
        arisreport,         // шлак
        aristemplate,       // шлак
        idoc,
        rfcmsg,             // встречалось пока только в ссылках
        ifmextcmplx,        // встречалось пока только в ссылках
        icmplx,             // встречалось пока только в ссылках
        ifmcontobj,
        ifmextdef,
        ifmfaultm,
        ifmmessage,
        ifmmessif,
        ifmextmes,          // встречалось пока лишь в ссылках
        ifmoper,
        ifmtypedef,         // дата тип
        ifmtypeenh,
        ifmuitexts,
        imsg,
        iseg,
        ityp,
        process,            // не парсить
        processstep,        // не парсить
        rfc,
        swc,
        type,
        BO_Query,           // встречалось пока только в ссылках
        BO_Object,          // navi
        BO_Enh,             // navi
        agent,              // navi

        // Directory
        Party,
        Channel,
        Service,
        AllInOne,
        DirectoryView,
        AlertRule,
        OutboundBinding,
        InboundBinding,
        RoutingRule
    }

    companion object {
        val dir75alltypes = listOf(
            ETypeID.Party, ETypeID.Channel, ETypeID.Service, ETypeID.AllInOne, ETypeID.DirectoryView,
            ETypeID.AlertRule, ETypeID.OutboundBinding, ETypeID.InboundBinding, ETypeID.RoutingRule
        )
    }
}