package k5

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

// Сервисы из http://sap.com/xi/BASIS

@Serializable
class LogMessageCollection(
    @XmlElement(true)
    @XmlSerialName("LogMessageCommunicationChannel", "", "")
    val channelLogs: MutableList<LogMessage> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageOthers", "", "")
    val otherLogs: MutableList<LogMessage> = mutableListOf(),
)

@Serializable
// нельзя здесь указывать @XmlSerialName! он определяется по месту вызова
class LogMessage(
    @XmlElement(true)
    @XmlSerialName("LogMessageItem", "", "")
    val LogMessageItem: MutableList<LogMessageItem> = mutableListOf()
)

@Serializable
class LogMessageItem(
    @XmlElement(true)
    @XmlSerialName("SeverityCode", "", "")
    val SeverityCode: Int,
    @XmlElement(true)
    @XmlSerialName("ClassificationCode", "", "")
    val ClassificationCode: String,
    @XmlElement(true)
    @XmlSerialName("Message", "", "")
    val Message: String? = null
)


// {{host}}/CommunicationChannelInService/CommunicationChannelInImplBean
@Serializable
@XmlSerialName("CommunicationChannelQueryRequest", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelQueryRequest {
    companion object {
        fun getUrl(host: String) = "$host/CommunicationChannelInService/CommunicationChannelInImplBean"
    }

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("CommunicationChannelQueryResponse", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelQueryResponse(
    @XmlElement(true)
    @XmlSerialName("CommunicationChannelID", "", "")
    val channels: MutableList<CommunicationChannelID> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
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
// @XmlSerialName использовать запрещено
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
    @XmlSerialName("CommunicationChannelID", "", "")
    val channel: MutableList<CommunicationChannelID> = mutableListOf()
) {
    companion object {
        fun getUrl(host: String) = "$host/CommunicationChannelInService/CommunicationChannelInImplBean"
    }

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("CommunicationChannelReadResponse", "http://sap.com/xi/BASIS", "b")
class CommunicationChannelReadResponse(
    @XmlElement(true)
    val channels: List<CommunicationChannel> = listOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<CommunicationChannelReadResponse>>(payloadXml).data
    }
}

enum class CommunicationChannelDirectionEnum { Sender, Receiver }

@Serializable
@XmlSerialName("Description", "", "")
data class Description(
    val languageCode: String,
    @XmlValue(true)
    val text: String? = null
)

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
class ValueMappingQueryRequest {
    companion object {
        fun getUrl(host: String) = "$host/ValueMappingInService/ValueMappingInImplBean"
    }

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("ValueMappingQueryResponse", "http://sap.com/xi/BASIS", "b")
class ValueMappingQueryResponse(
    @XmlElement(true)
    @XmlSerialName("ValueMappingID", "", "")
    val ValueMappingID: MutableList<String> = mutableListOf(),
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

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("ValueMappingReadResponse", "http://sap.com/xi/BASIS", "b")
class ValueMappingReadResponse(
    @XmlElement(true)
    val ValueMapping: MutableList<ValueMapping> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
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
class ConfigurationScenarioQueryRequest {
    companion object {
        fun getUrl(host: String) = "$host/ConfigurationScenarioInService/ConfigurationScenarioInImplBean"
    }

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("ConfigurationScenarioQueryResponse", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenarioQueryResponse(
    @XmlElement(true)
    @XmlSerialName("ConfigurationScenarioID", "", "")
    val ConfigurationScenarioID: MutableList<String> = mutableListOf(),

    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
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

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("ConfigurationScenarioReadResponse", "http://sap.com/xi/BASIS", "b")
class ConfigurationScenarioReadResponse(
    @XmlElement(true)
    @XmlSerialName("ConfigurationScenario", "", "")
    val ConfigurationScenario: MutableList<ConfigurationScenario> = mutableListOf(),

    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
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
    @XmlSerialName("IntegratedConfiguration", "", "")
    val IntegratedConfiguration: MutableList<IntegratedConfigurationID> = mutableListOf()
)

@Serializable
@XmlSerialName("IntegratedConfigurationQueryRequest", "http://sap.com/xi/BASIS", "b")
class IntegratedConfigurationQueryRequest {
    companion object {
        fun getUrl750(host: String) = "$host/IntegratedConfiguration750InService/IntegratedConfiguration750InImplBean"
    }

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("IntegratedConfigurationQueryResponse", "http://sap.com/xi/BASIS", "b")
class IntegratedConfigurationQueryResponse(
    @XmlElement(true)
    @XmlSerialName("IntegratedConfigurationID", "", "")
    val IntegratedConfigurationID: MutableList<IntegratedConfigurationID> = mutableListOf(),

    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<IntegratedConfigurationQueryResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("IntegratedConfigurationReadRequest", "http://sap.com/xi/BASIS", "b")
class IntegratedConfigurationReadRequest(
    @XmlElement(true)
    @XmlSerialName("ReadContext", "", "")
    val ReadContext: String = "User",
    @XmlElement(true)
    @XmlSerialName("IntegratedConfigurationID", "", "")
    val IntegratedConfigurationID: MutableList<IntegratedConfigurationID> = mutableListOf()
) {
    companion object {
        fun getUrl750(host: String) = "$host/IntegratedConfiguration750InService/IntegratedConfiguration750InImplBean"
    }

    fun composeSOAP() = xml().encodeToString(Envelope(this))
}

@Serializable
@XmlSerialName("IntegratedConfiguration750ReadResponse", "http://sap.com/xi/BASIS", "b")
class IntegratedConfiguration750ReadResponse(
    @XmlElement(true)
    @XmlSerialName("IntegratedConfiguration", "", "")
    val IntegratedConfiguration: MutableList<IntegratedConfiguration> = mutableListOf(),

    @XmlElement(true)
    @XmlSerialName("LogMessageCollection", "", "")
    val LogMessageCollection: LogMessageCollection
) {
    companion object {
        fun parse(payloadXml: String) =
            xml().decodeFromString<Envelope<IntegratedConfiguration750ReadResponse>>(payloadXml).data
    }
}

@Serializable
@XmlSerialName("IntegratedConfiguration", "http://sap.com/xi/BASIS", "b")
class IntegratedConfiguration(
    @XmlElement(true)
    val MasterLanguage: String,
    @XmlElement(true)
    val AdministrativeData: AdministrativeData,
    @XmlElement(true)
    val Description: Description?,
    @XmlElement(true)
    @XmlSerialName("IntegratedConfigurationID", "", "")
    val IntegratedConfigurationID: IntegratedConfigurationID,
    @XmlElement(true)
    val InboundProcessing: InboundProcessing,
    @XmlElement(true)
    val Receivers: Receivers,
    @XmlElement(true)
    @XmlSerialName("ReceiverInterfaces", "", "")
    val ReceiverInterfaces: MutableList<ReceiverInterfaces> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("OutboundProcessing", "", "")
    val OutboundProcessing: MutableList<OutboundProcessing> = mutableListOf(),
    @XmlElement(true)
    val PrefixNamespaceMapping: PrefixNamespaceMapping? = null,
    @XmlElement(true)
    @XmlSerialName("Logging", "", "")
    val Logging: StagingLogging,
    @XmlElement(true)
    @XmlSerialName("Staging", "", "")
    val Staging: StagingLogging,
)

@Serializable
@XmlSerialName("PrefixNamespaceMapping", "", "")
class PrefixNamespaceMapping(
    @XmlElement(true)
    val URI: String,
    @XmlElement(true)
    val Prefix: String,
)

@Serializable
@XmlSerialName("InboundProcessing", "", "")
class InboundProcessing(
    @XmlElement(true)
    @XmlSerialName("SenderInterfaceSoftwareComponentVersion", "", "")
    val SenderInterfaceSoftwareComponentVersion: String? = null,
    @XmlElement(true)
    @XmlSerialName("CommunicationChannel", "", "")
    val CommunicationChannel: CommunicationChannelID,
    @XmlElement(true)
    @XmlSerialName("SchemaValidationIndicator", "", "")
    val SchemaValidationIndicator: Boolean,
    @XmlElement(true)
    @XmlSerialName("VirusScan", "", "")
    val VirusScan: String,
    @XmlElement(true)
    @XmlSerialName("AdapterSpecificAttribute", "", "")
    val AdapterSpecificAttribute: CommunicationChannel.AdapterSpecificAttribute? = null,
    @XmlElement(true)
    @XmlSerialName("AssignedUser", "", "")
    val AssignedUser: String? = null
)

@Serializable
@XmlSerialName("Receivers", "", "")
class Receivers(
    @XmlElement(true)
    @XmlSerialName("ReceiverWildcardIndicator", "", "")
    val ReceiverWildcardIndicator: Boolean,
    @XmlElement(true)
    @XmlSerialName("DynamicReceiverRule", "", "")
    val DynamicReceiverRule: DynamicReceiverRule? = null,
    @XmlElement(true)
    @XmlSerialName("ReceiverRule", "", "")
    val ReceiverRule: MutableList<ReceiverRule> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("NoReceiverBehaviour", "", "")
    val NoReceiverBehaviour: String,
    @XmlElement(true)
    @XmlSerialName("NoReceiverReceiver", "", "")
    val NoReceiverReceiver: Receiver? = null
)

@Serializable
class DynamicReceiverRule(
    @XmlElement(true)
    val Operation: String? = null,
    @XmlElement(true)
    @XmlSerialName("Mapping", "", "")
    val Mapping: RepositoryReferenceID,
    @XmlElement(true)
    @XmlSerialName("MappingParamters", "", "")
    val MappingParamters: MutableList<MappingParameters> = mutableListOf()
)

@Serializable
@XmlSerialName("ReceiverRule", "", "")
class ReceiverRule(
    @XmlElement(true)
    @XmlSerialName("Condition", "", "")
    val Condition: ReceiverRuleCondition,
    @XmlElement(true)
    @XmlSerialName("Receiver", "", "")
    val Receiver: Receiver,
)

@Serializable
@XmlSerialName("Condition", "", "")
class ReceiverRuleCondition(
    @XmlElement(true)
    @XmlSerialName("AtomicConditionBlock", "", "")
    val AtomicConditionBlock: MutableList<AtomicConditionBlock> = mutableListOf(),
)

@Serializable
@XmlSerialName("AtomicConditionBlock", "", "")
class AtomicConditionBlock(
    @XmlElement(true)
    @XmlSerialName("AtomicCondition", "", "")
    val AtomicCondition: AtomicCondition,
)

@Serializable
@XmlSerialName("AtomicCondition", "", "")
class AtomicCondition(
    @XmlElement(true)
    @XmlSerialName("Operator", "", "")
    val Operator: String,   //TODO - make ENUM
    @XmlElement(true)
    @XmlSerialName("LeftExtractor", "", "")
    val LeftExtractor: AtomicConditionExtractor,
    @XmlElement(true)
    @XmlSerialName("RightExtractor", "", "")
    val RightExtractor: AtomicConditionExtractor
)

@Serializable
@XmlSerialName("AtomicConditionExtractor", "", "")
class AtomicConditionExtractor(
    @XmlElement(true)
    val TypeID: String, //TODO - make Enum
    @XmlElement(true)
    val Value: String,
    @XmlElement(true)
    val Datatype: String,
    @XmlElement(true)
    val ContextObjectName: String,
    @XmlElement(true)
    val ContextObjectNamespace: String
)

@Serializable
@XmlSerialName("Receiver", "", "")
class Receiver(
    @XmlElement(true)
    @XmlSerialName("CommunicationParty", "", "")
    val CommunicationParty: ReceiverParam,
    @XmlElement(true)
    @XmlSerialName("CommunicationPartySchema", "", "")
    val CommunicationPartySchema: ReceiverParam,
    @XmlElement(true)
    @XmlSerialName("CommunicationPartyAgency", "", "")
    val CommunicationPartyAgency: ReceiverParam,
    @XmlElement(true)
    @XmlSerialName("CommunicationComponent", "", "")
    val CommunicationComponent: ReceiverParam,
)

@Serializable
class ReceiverParam(
    @XmlElement(true)
    val TypeID: String,
    @XmlElement(true)
    val Value: String,
    @XmlElement(true)
    val Datatype: String,
    @XmlElement(true)
    val ContextObjectName: String,
    @XmlElement(true)
    val ContextObjectNamespace: String
)


@Serializable
@XmlSerialName("ReceiverInterfaces", "", "")
class ReceiverInterfaces(
    @XmlElement(true)
    @XmlSerialName("Receiver", "", "")
    val Receiver: BusinessSystemID,
    @XmlElement(true)
    @XmlSerialName("ReceiverInterfaceRule", "", "")
    val ReceiverInterfaceRule: ReceiverInterfaceRule,
    @XmlElement(true)
    @XmlSerialName("QualityOfService", "", "")
    val QualityOfService: String,   //TODO - ENUM. EO, EOIO and what else?
)

@Serializable
@XmlSerialName("ReceiverInterfaceRule", "", "")
class ReceiverInterfaceRule(
    @XmlElement(true)
    @XmlSerialName("Operation", "", "")
    val Operation: String,
    @XmlElement(true)
    @XmlSerialName("Condition", "", "")
    val Condition: ReceiverRuleCondition? = null,
    @XmlElement(true)
    @XmlSerialName("Mapping", "", "")
    val Mapping: RepositoryReferenceID? = null,
    @XmlElement(true)
    @XmlSerialName("MappingParameters", "", "")
    val MappingParameters: MutableList<MappingParameters> = mutableListOf(),
    @XmlElement(true)
    @XmlSerialName("Interface", "", "")
    val Interface: RepositoryReferenceID,
)

@Serializable
class MappingParameters(
    @XmlElement(true)
    @XmlSerialName("String", "", "")
    val String: MappingParameterString? = null,
    @XmlElement(true)
    @XmlSerialName("Integer", "", "")
    val Integer: MappingParameterString? = null
)

@Serializable
class MappingParameterString(
    @XmlElement(true)
    @XmlSerialName("Name", "", "")
    val Name: String,
    @XmlElement(true)
    @XmlSerialName("Value", "", "")
    val Value: String,
)

@Serializable
class RepositoryReferenceID(
    @XmlElement(true)
    val Name: String,
    @XmlElement(true)
    val Namespace: String,
    @XmlElement(true)
    val SoftwareComponentVersionID: String,
)


@Serializable
@XmlSerialName("OutboundProcessing", "", "")
class OutboundProcessing(
    @XmlElement(true)
    @XmlSerialName("Receiver", "", "")
    val Receiver: BusinessSystemID,
    @XmlElement(true)
    @XmlSerialName("ReceiverInterface", "", "")
    val ReceiverInterface: RepositoryReferenceID,
    @XmlElement(true)
    @XmlSerialName("CommunicationChannel", "", "")
    val CommunicationChannel: CommunicationChannelID,
    @XmlElement(true)
    @XmlSerialName("SchemaValidationIndicator", "", "")
    val SchemaValidationIndicator: Boolean,
    @XmlElement(true)
    @XmlSerialName("VirusScan", "", "")
    val VirusScan: String,
    @XmlElement(true)
    @XmlSerialName("AdapterSpecificAttribute", "", "")
    val AdapterSpecificAttribute: CommunicationChannel.AdapterSpecificAttribute? = null,
    @XmlElement(true)
    @XmlSerialName("HeaderMapping", "", "")
    val HeaderMapping: HeaderMapping,
)

@Serializable
class HeaderMapping(
    @XmlElement(true)
    @XmlSerialName("Sender", "", "")
    val Sender: CommunicationComponent,
    @XmlElement(true)
    @XmlSerialName("Receiver", "", "")
    val Receiver: CommunicationComponent
)

@Serializable
class CommunicationComponent(
    @XmlElement(true)
    @XmlSerialName("CommunicationParty", "", "")
    val CommunicationParty: ReceiverParam? = null,
    @XmlElement(true)
    @XmlSerialName("CommunicationPartySchema", "", "")
    val CommunicationPartySchema: ReceiverParam? = null,
    @XmlElement(true)
    @XmlSerialName("CommunicationPartyAgency", "", "")
    val CommunicationPartyAgency: ReceiverParam? = null,
    @XmlElement(true)
    @XmlSerialName("CommunicationComponent", "", "")
    val CommunicationComponent: ReceiverParam? = null
)


@Serializable
class StagingLogging(
    @XmlElement(true)
    @XmlSerialName("UseGlobal", "", "")
    val UseGlobal: Boolean,
    @XmlElement(true)
    @XmlSerialName("SpecificConfiguration", "", "")
    val SpecificConfiguration: String? = null
)