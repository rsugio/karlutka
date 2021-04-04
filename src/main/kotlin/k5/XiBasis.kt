package k5

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.io.Reader

// Сервисы из http://sap.com/xi/BASIS


// {{host}}/CommunicationChannelInService/CommunicationChannelInImplBean
@Serializable
@XmlSerialName("CommunicationChannelQueryRequest", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelQueryRequest(
) {
    companion object {
        fun getUrl(host: String) = "$host/CommunicationChannelInService/CommunicationChannelInImplBean"
    }
}

@Serializable
@XmlSerialName("CommunicationChannelQueryResponse", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelQueryResponse(
    @XmlElement(true)
    @XmlSerialName("CommunicationChannelID", "", "")
    val channels: MutableList<CommunicationChannelID> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: String? = null
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<CommunicationChannelQueryResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("BusinessSystem", "", "")
data class BusinessSystemID(
    @XmlElement(true)
    val PartyID: String,
    @XmlElement(true)
    val ComponentID: String,
)

@Serializable
// @XmlSerialName здесь не указывать! оно определяется по месту вызова
data class CommunicationChannelID(
    @XmlElement(true)
    val PartyID: String,
    @XmlElement(true)
    val ComponentID: String,
    @XmlElement(true)
    val ChannelID: String
)

@Serializable
@XmlSerialName("IntegratedConfiguration", "", "")
data class IntegratedConfigurationID(
    @XmlElement(true)
    val SenderPartyID: String,
    @XmlElement(true)
    val SenderComponentID: String,
    @XmlElement(true)
    val InterfaceName: String,
    @XmlElement(true)
    val InterfaceNamespace: String,
    @XmlElement(true)
    val ReceiverPartyID: String,
    @XmlElement(true)
    val ReceiverComponentID: String
)

@Serializable
@XmlSerialName("CommunicationChannelReadRequest", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelReadRequest(
    @XmlElement(true)
    @XmlSerialName("ReadContext", "", "")
    val ReadContext: String = "User",
    @XmlElement(true)
    val channel: MutableList<CommunicationChannelID> = mutableListOf()
) {
    companion object {
        fun getUrl(host: String) = "$host/CommunicationChannelInService/CommunicationChannelInImplBean"
    }
}

@Serializable
@XmlSerialName("CommunicationChannelReadResponse", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelReadResponse(
    @XmlElement(true)
    val channels: List<CommunicationChannel> = listOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: String? = null
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<CommunicationChannelReadResponse>>(payloadXml).data
    }
}

enum class CommunicationChannelDirectionEnum { Sender, Receiver }

@Serializable
@XmlSerialName("CommunicationChannel", "", "")
data class CommunicationChannel(
    @XmlElement(true)
    val MasterLanguage: String,
    @XmlElement(true)
    val AdministrativeData: AdministrativeData,
    @XmlElement(true)
    val Description: String = "",
    @XmlElement(true)
    val Channel: CommunicationChannelID,
    @XmlElement(true)
    val adapterMetadata: AdapterMetadata,
    @XmlElement(true)
    @XmlSerialName("Direction", "", "")
    val Direction: CommunicationChannelDirectionEnum, //Receiver
    @XmlElement(true)
    val TransportProtocol: String, //HTTP
    @XmlElement(true)
    val TransportProtocolVersion: String?, //HTTP
    @XmlElement(true)
    val MessageProtocol: String, //SOAP
    @XmlElement(true)
    val MessageProtocolVersion: String, //SOAP
    @XmlElement(true)
    val AdapterEngineName: String, //af.dpz.ld-s-poddb
    @XmlElement(true)
    val adapterSpecificAttribute: List<AdapterSpecificAttribute> = listOf(),
    @XmlElement(true)
    val adapterSpecificTableAttribute: List<AdapterSpecificTableAttribute> = listOf(),
    @XmlElement(true)
    val modules: ModuleProcess,
    @XmlElement(true)
    @XmlSerialName("SenderIdentifier", "", "")
    val senderIdentifier: Identifier,
    @XmlSerialName("ReceiverIdentifier", "", "")
    val receiverIdentifier: Identifier,
) {
    @Serializable
    data class AdapterMetadata(
        @XmlElement(true)
        val Name: String, //SOAP
        @XmlElement(true)
        val Namespace: String, //http://sap.com/xi/XI/System
        @XmlElement(true)
        val SoftwareComponentVersionID: String, //0050568f-0aac-1ed4-a6e5-6926325e2eb3
    )

    @Serializable
    data class AdapterSpecificAttribute(
        @XmlElement(true)
        val Name: String, //enableDynConfigReceiver
        @XmlElement(true)
        val Namespace: String, //?
        @XmlElement(true)
        val Value: String
    )

    @Serializable
    data class AdapterSpecificTableAttribute(
        @XmlElement(true)
        val Name: String, //enableDynConfigReceiver
        @XmlElement(true)
        val Namespace: String? = null, //?
        @XmlElement(true)
        val Value: String? = null, //?
        @XmlElement(true)
        val valueTable: ValueTableRow? = null
    ) {
        @Serializable
        data class ValueTableRow(
            @XmlElement(true)
            val valueTableCell: MutableList<ValueTableCell> = mutableListOf()
        )

        @Serializable
        data class ValueTableCell(
            @XmlElement(true)
            val ColumnName: String,
            @XmlElement(true)
            val Value: String
        )
    }

    @Serializable
    @XmlSerialName("ModuleProcess", "", "")
    data class ModuleProcess(
        val process: MutableList<ProcessStep> = mutableListOf(),
        val params: MutableList<ParameterGroup> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("ProcessStep", "", "")
    data class ProcessStep(
        @XmlElement(true)
        val ModuleName: String, //sap.com/com.sap.aii.af.soapadapter/XISOAPAdapterBean
        @XmlElement(true)
        val ModuleType: String, //Local Enterprise Bean
        @XmlElement(true)
        val ParameterGroupID: String, //soap
    )

    @Serializable
    @XmlSerialName("ParameterGroup", "", "")
    data class ParameterGroup(
        @XmlElement(true)
        val ParameterGroupID: String, //request
        @XmlElement(true)
        val parameters: MutableList<Parameter> = mutableListOf()
    )

    @Serializable
    data class Parameter(
        @XmlElement(true)
        val Name: String = "", //interfaceNamespace
        @XmlElement(true)
        val Value: String = "" //urn:asdfgh
    )
}

// у идентификаторов динамическое имя, сюда не пишем
@Serializable
data class Identifier(
    val schemeAgencyID: String = "", //sap.com/com.sap.aii.af.soapadapter/XISOAPAdapterBean
    val schemeID: String = "",       //Local Enterprise Bean
    @XmlValue(true)
    inline val text: String = ""
)

@Serializable
@XmlSerialName("AdministrativeData", "", "")
data class AdministrativeData(
    @XmlElement(true)
    val ResponsibleUserAccountID: String, //ivanov
    @XmlElement(true)
    val LastChangeUserAccountID: String, //petrov
    @XmlElement(true)
    val LastChangeDateTime: String, //2020-04-28T11:18:51.118+03:00
    @XmlElement(true)
    val FolderPathID: String, ///SCPI/SAP_HYBRIS/Entities/SendStations/
)

@Serializable
@XmlSerialName("ValueMappingQueryRequest", "http://sap.com/xi/BASIS", "b")
class ValueMappingQueryRequest(
) {
    companion object {
        fun getUrl(host: String) = "$host/ValueMappingInService/ValueMappingInImplBean"
    }
}

@Serializable
@XmlSerialName("ValueMappingQueryResponse", "http://sap.com/xi/BASIS", "b")
class ValueMappingQueryResponse(
    @XmlElement(true)
    @XmlSerialName("ValueMappingID", "", "")
    val channel: MutableList<String> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: String?
) {
    companion object {
        fun parse(payloadXml: String) = xml().decodeFromString<Envelope<ValueMappingQueryResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("ValueMappingReadRequest", "http://sap.com/xi/BASIS", "b")
class ValueMappingReadRequest(
    @XmlElement(true)
    @XmlSerialName("ReadContext", "", "")
    val ReadContext: String? = null,
    @XmlElement(true)
    @XmlSerialName("ValueMappingID", "", "")
    val channel: MutableList<String> = mutableListOf()
) {
    companion object {
        fun getUrl(host: String) = "$host/ValueMappingInService/ValueMappingInImplBean"
    }
}

@Serializable
@XmlSerialName("ValueMappingReadResponse", "http://sap.com/xi/BASIS", "b")
class ValueMappingReadResponse(
    @XmlElement(true)
    val ValueMapping: MutableList<ValueMapping> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: String?
) {
    companion object {
        fun parse(payloadXml: String) = xml().decodeFromString<Envelope<ValueMappingReadResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("ValueMapping", "", "")
class ValueMapping(
    @XmlElement(true)
    val MasterLanguage: String,
    val AdministrativeData: AdministrativeData,
    @XmlElement(true)
    val ValueMappingID: String,
    @XmlElement(true)
    val GroupName: String,
    @XmlElement(true)
    @XmlSerialName("Representation", "", "")
    val Representation: MutableList<Identifier> = mutableListOf(),
)

@Serializable
@XmlSerialName("ConfigurationScenarioQueryRequest", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenarioQueryRequest(
) {
    companion object {
        fun getUrl(host: String) = "$host/ConfigurationScenarioInService/ConfigurationScenarioInImplBean"
    }
}

@Serializable
@XmlSerialName("ConfigurationScenarioQueryResponse", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenarioQueryResponse(
    @XmlElement(true)
    @XmlSerialName("ConfigurationScenarioID", "", "")
    val ConfigurationScenarioID: MutableList<String> = mutableListOf(),

    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: String? = null
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<ConfigurationScenarioQueryResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("ConfigurationScenarioReadRequest", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenarioReadRequest(
    @XmlElement(true)
    @XmlSerialName("ReadContext", "", "")
    val ReadContext: String,

    @XmlElement(true)
    @XmlSerialName("ConfigurationScenarioID", "", "")
    val ConfigurationScenarioID: MutableList<String> = mutableListOf(),
) {
    companion object {
        fun getUrl(host: String) = "$host/ConfigurationScenarioInService/ConfigurationScenarioInImplBean"
    }
}

@Serializable
@XmlSerialName("ConfigurationScenarioReadResponse", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenarioReadResponse(
    @XmlElement(true)
    @XmlSerialName("ConfigurationScenario", "", "")
    val ConfigurationScenario: MutableList<ConfigurationScenario> = mutableListOf(),

    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: String? = null
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<ConfigurationScenarioReadResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("ConfigurationScenario", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenario(
    @XmlElement(true)
    val MasterLanguage: String,
    @XmlElement(true)
    val AdministrativeData: AdministrativeData,
    @XmlElement(true)
    val ConfigurationScenarioID: String,
    @XmlElement(true)
    val BusinessSystem: MutableList<BusinessSystemID> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("CommunicationChannel", "", "")
    val CommunicationChannel: MutableList<CommunicationChannelID> = mutableListOf(),
    @XmlElement(true)
    val IntegratedConfiguration: MutableList<IntegratedConfigurationID> = mutableListOf()
)