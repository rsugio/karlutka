package k5

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.net.ssl.HttpsURLConnection
import kotlin.system.exitProcess

enum class EnumDirection { INBOUND, OUTBOUND }

@Serializable
@XmlSerialName("getConnections", "urn:AdapterMessageMonitoringVi", "y")
class getConnections()

@Serializable
@XmlSerialName("getConnectionsResponse", "urn:AdapterMessageMonitoringVi", "y")
class getConnectionsResponse(
    val Response: Resp
) {
    @Serializable
    @XmlSerialName("Response", "", "")
    class Resp(
        @XmlSerialName("String", "urn:java/lang", "rn6")
        val x: List<String>
    )
}

@Serializable
@XmlSerialName("getMessageBytesJavaLangStringIntBoolean", "urn:AdapterMessageMonitoringVi", "y")
class getMessageBytesJavaLangStringIntBoolean(
    @XmlElement(true)
    @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "y")
    val messageKey: String = "",
    @XmlElement(true)
    @XmlSerialName("version", "urn:AdapterMessageMonitoringVi", "y")
    val version: Int = 0,
    @XmlElement(true)
    @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "y")
    val archive: Boolean = false
)

@Serializable
@XmlSerialName("getMessageBytesJavaLangStringIntBooleanResponse", "urn:AdapterMessageMonitoringVi", "y")
class getMessageBytesJavaLangStringIntBooleanResponse(
    @XmlElement(true)
    @XmlSerialName("Response", "", "")
    val Response: String = ""
)

@Serializable
@XmlSerialName("getMessagesByIDs", "urn:AdapterMessageMonitoringVi", "y")
class getMessagesByIDs(
    @XmlSerialName("messageIds", "urn:AdapterMessageMonitoringVi", "y")
    val messageIds: ListOfStrings = ListOfStrings(),
    @XmlSerialName("referenceIds", "urn:AdapterMessageMonitoringVi", "y")
    val referenceIds: ListOfStrings = ListOfStrings(),
    @XmlSerialName("correlationIds", "urn:AdapterMessageMonitoringVi", "y")
    val correlationIds: ListOfStrings = ListOfStrings(),
    @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "y")
    val archive: Boolean = false
) {
    @Serializable
    class ListOfStrings(
        @XmlElement(true)
        @XmlSerialName("String", "urn:java/lang", "la")
        val String: List<String> = listOf()
    )
}

@Serializable
@XmlSerialName("getMessagesByIDsResponse", "urn:AdapterMessageMonitoringVi", "y")
class getMessagesByIDsResponse(
    val Resp: Response = Response(getMessageListResponse.LAFW(listOf()))
) {
    @Serializable
    @XmlSerialName("Response", "", "")
    class Response(
        @XmlElement(true)
        val afw: getMessageListResponse.LAFW,
        @XmlElement(true)
        @XmlSerialName("number", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val number: Int = 0,
        @XmlElement(true)
        @XmlSerialName("warning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val warning: Boolean = false,
        @XmlElement(true)
        @XmlSerialName("displayPermissionWarning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val displayPermissionWarning: Boolean = false
    )
}

@Serializable
@XmlSerialName("getMessageList", "urn:AdapterMessageMonitoringVi", "y")
class getMessageList(
    @XmlElement(true)
    @XmlSerialName("filter", "urn:AdapterMessageMonitoringVi", "y")
    val filter: Filter = Filter(),
    @XmlElement(true)
    @XmlSerialName("maxMessages", "urn:AdapterMessageMonitoringVi", "y")
    val maxMessages: Int = 9999
) {
    @Serializable
    class Filter(
        @XmlElement(true)
        @XmlSerialName("archive", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val archive: Boolean = false,
        @XmlElement(true)
        @XmlSerialName("dateType", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val dateType: String = "",
        @XmlElement(true)
        @XmlSerialName("nodeId", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val nodeId: Long = 0,
        @XmlElement(true)
        @XmlSerialName("onlyFaultyMessages", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val onlyFaultyMessages: Boolean = false,
        @XmlElement(true)
        @XmlSerialName("retries", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val retries: Int = 0,
        @XmlElement(true)
        @XmlSerialName("retryInterval", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val retryInterval: Int = 0,
        @XmlElement(true)
        @XmlSerialName("timesFailed", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val timesFailed: Int = 0,
        @XmlElement(true)
        @XmlSerialName("wasEdited", "urn:com.sap.aii.mdt.server.adapterframework.ws", "pns")
        val wasEdited: Boolean = false
    )
}

@Serializable
@XmlSerialName("getMessageListResponse", "urn:AdapterMessageMonitoringVi", "y")
class getMessageListResponse(val Resp: Response) {
    @Serializable
    @XmlSerialName("Response", "", "")
    class Response(
        @XmlElement(true)
        @XmlSerialName("date", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val date: String,
        @XmlElement(true)
        val afw: LAFW,
        @XmlElement(true)
        @XmlSerialName("number", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val number: Int,
        @XmlElement(true)
        @XmlSerialName("warning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val warning: Boolean,
        @XmlElement(true)
        @XmlSerialName("displayPermissionWarning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val displayPermissionWarning: Boolean
    )

    @Serializable
    @XmlSerialName("list", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    class LAFW(val list: List<AdapterFrameworkData>)
}

@Serializable
@XmlSerialName("AdapterFrameworkData", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
class AdapterFrameworkData(
    @XmlSerialName("cancelable", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val cancelable: rn2Boolean = rn2Boolean(false),
    @XmlElement(true)
    @XmlSerialName("connectionName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val connectionName: String = "",
    @XmlElement(true)
    @XmlSerialName("credential", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val credential: String = "",
    @XmlElement(true)
    @XmlSerialName("direction", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val direction: EnumDirection = EnumDirection.OUTBOUND,
    @XmlSerialName("editable", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val editable: rn2Boolean = rn2Boolean(false),
    @XmlElement(true)
    @XmlSerialName("endTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val endTime: String = "",
    @XmlElement(true)
    @XmlSerialName("endpoint", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val endpoint: String = "",
    @XmlElement(true)
    @XmlSerialName("headers", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val headers: String = "",
    @XmlSerialName("interface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val interface_: Interface_ = Interface_("SI_Sync", "urn:"),
    @XmlElement(true)
    @XmlSerialName("isPersistent", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val isPersistent: Boolean = false,
    @XmlElement(true)
    @XmlSerialName("messageID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val messageID: String = "",
    @XmlElement(true)
    @XmlSerialName("messageKey", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val messageKey: String = "",
    @XmlElement(true)
    @XmlSerialName("messageType", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val messageType: String = "messageType",
    @XmlElement(true)
    @XmlSerialName("nodeId", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val nodeId: Long = 0,
    @XmlElement(true)
    @XmlSerialName("persistUntil", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val persistUntil: String = "",
    @XmlElement(true)
    @XmlSerialName("protocol", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val protocol: String = "",
    @XmlElement(true)
    @XmlSerialName("qualityOfService", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val qualityOfService: String = "",
    @XmlSerialName("receiverInterface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val receiverInterface: Interface_ = Interface_("SI_Sync", "urn:"),
    @XmlElement(true)
    @XmlSerialName("receiverName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val receiverName: String = "",
    @XmlSerialName("receiverParty", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val receiverParty: Party_ = Party_("http://sap.com/xi/XI", "", "XIParty"),
    @XmlElement(true)
    @XmlSerialName("referenceID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val referenceID: String = "",
    @XmlSerialName("restartable", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val restartable: rn2Boolean = rn2Boolean(false),
    @XmlElement(true)
    @XmlSerialName("retries", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val retries: Int = 0,
    @XmlElement(true)
    @XmlSerialName("retryInterval", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val retryInterval: Int = 0,
    @XmlElement(true)
    @XmlSerialName("scheduleTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val scheduleTime: String = "",
    @XmlSerialName("senderInterface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val senderInterface: Interface_ = Interface_("SI_Sync", "urn:"),
    @XmlElement(true)
    @XmlSerialName("senderName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val senderName: String = "senderName",
    @XmlSerialName("senderParty", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val senderParty: Party_ = Party_("http://sap.com/xi/XI", "", "XIParty"),
    @XmlElement(true)
    @XmlSerialName("sequenceNumber", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val sequenceNumber: Int = 0,
    @XmlElement(true)
    @XmlSerialName("serializationContext", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val serializationContext: String = "serializationContext",
    @XmlElement(true)
    @XmlSerialName("startTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val startTime: String = "startTime",
    @XmlElement(true)
    @XmlSerialName("status", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val status: String = "status",
    @XmlElement(true)
    @XmlSerialName("timesFailed", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val timesFailed: Int = 0,
    @XmlElement(true)
    @XmlSerialName("transport", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val transport: String = "transport",
    @XmlElement(true)
    @XmlSerialName("validUntil", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val validUntil: String = "validUntil",
    @XmlElement(true)
    @XmlSerialName("version", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val version: Int = 0,
    @XmlSerialName("wasEdited", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val wasEdited: rn2Boolean = rn2Boolean(false),
    @XmlElement(true)
    @XmlSerialName("payloadPermissionWarning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val payloadPermissionWarning: String = "payloadPermissionWarning",
    @XmlElement(true)
    @XmlSerialName("errorLabel", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val errorLabel: Int = 0,
    @XmlElement(true)
    @XmlSerialName("scenarioIdentifier", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val scenarioIdentifier: String = "dir://ICO/f",
    @XmlSerialName("duration", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val duration: rn2Duration = rn2Duration(999),
    @XmlElement(true)
    @XmlSerialName("size", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val size: Long = 0,
    @XmlElement(true)
    @XmlSerialName("messagePriority", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
    val messagePriority: Long = 0,
)

@Serializable
class Interface_(
    @XmlElement(true)
    @XmlSerialName("name", "urn:com.sap.aii.mdt.api.data", "rn2")
    val name: String = "",
    @XmlElement(true)
    @XmlSerialName("namespace", "urn:com.sap.aii.mdt.api.data", "rn2")
    val namespace: String = ""
)

@Serializable
class Party_(
    @XmlElement(true)
    @XmlSerialName("agency", "urn:com.sap.aii.mdt.api.data", "rn2")
    val agency: String = "",
    @XmlElement(true)
    @XmlSerialName("name", "urn:com.sap.aii.mdt.api.data", "rn2")
    val name: String = "",
    @XmlElement(true)
    @XmlSerialName("schema", "urn:com.sap.aii.mdt.api.data", "rn2")
    val schema: String = "",
)

@Serializable
class rn2Boolean(
    @XmlElement(true)
    @XmlSerialName("value", "urn:com.sap.aii.mdt.api.data", "rn2")
    val value: Boolean
)

@Serializable
class laString(
    @XmlElement(true)
    @XmlSerialName("String", "urn:java/lang", "la")
    val string: String = ""
)


@Serializable
class rn2Duration(
    @XmlElement(true)
    @XmlSerialName("duration", "urn:com.sap.aii.mdt.api.data", "rn2")
    val duration: Int
)

@Serializable
@XmlSerialName("getIntegrationFlows", "urn:AdapterMessageMonitoringVi", "y")
class getIntegrationFlows(
    @XmlElement(true)
    @XmlSerialName("language", "urn:AdapterMessageMonitoringVi", "y")
    val language: String = ""
)

@Serializable
@XmlSerialName("getIntegrationFlowsResponse", "urn:AdapterMessageMonitoringVi", "y")
class getIntegrationFlowsResponse(
    val Response: Resp
) {
    @Serializable
    @XmlSerialName("Response", "", "")
    class Resp(
        @XmlSerialName("IntegrationFlow", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val list: List<IntegrationFlow>
    )

    @Serializable
    class IntegrationFlow(
        @XmlElement(true)
        @XmlSerialName("name", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val name: String,
        @XmlElement(true)
        @XmlSerialName("description", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val description: String? = null,
        @XmlElement(true)
        @XmlSerialName("id", "urn:com.sap.aii.mdt.server.adapterframework.ws", "rn7")
        val id: String
    )
}


@Serializable
@XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "S")
class Envelope<BODYTYPE> private constructor(
    private val body: Body<BODYTYPE>
) {
    constructor(data: BODYTYPE) : this(Body(data))

    val data: BODYTYPE get() = body.data
    override fun toString(): String {
        return "Envelope(body=$body)"
    }

    @Serializable
    private class Body<BODYTYPE>(@Polymorphic val data: BODYTYPE)
}

fun xml(): XML {
    val module = SerializersModule {
        polymorphic(Any::class) {
            // AdapterMessageMonitoringViCommunicationChannelQueryResponse
            subclass(getConnections::class, serializer())
            subclass(getConnectionsResponse::class, serializer())
            subclass(getIntegrationFlows::class, serializer())
            subclass(getIntegrationFlowsResponse::class, serializer())
            subclass(getMessageList::class, serializer())
            subclass(getMessageListResponse::class, serializer())
            subclass(getMessageBytesJavaLangStringIntBoolean::class, serializer())
            subclass(getMessageBytesJavaLangStringIntBooleanResponse::class, serializer())
            subclass(getMessagesByIDs::class, serializer())
            subclass(getMessagesByIDsResponse::class, serializer())

            // XiBasis
            subclass(CommunicationChannelQueryRequest::class, serializer())
            subclass(CommunicationChannelQueryResponse::class, serializer())
            subclass(CommunicationChannelReadRequest::class, serializer())
            subclass(CommunicationChannelReadResponse::class, serializer())
            subclass(ValueMappingQueryRequest::class, serializer())
            subclass(ValueMappingQueryResponse::class, serializer())
            subclass(ValueMappingReadRequest::class, serializer())
            subclass(ValueMappingReadResponse::class, serializer())
            subclass(ConfigurationScenarioQueryRequest::class, serializer())
            subclass(ConfigurationScenarioQueryResponse::class, serializer())
            subclass(ConfigurationScenarioReadRequest::class, serializer())
            subclass(ConfigurationScenarioReadResponse::class, serializer())
            subclass(IntegratedConfigurationQueryRequest::class, serializer())
            subclass(IntegratedConfigurationQueryResponse::class, serializer())
            subclass(IntegratedConfigurationReadRequest::class, serializer())
            subclass(IntegratedConfiguration750ReadResponse::class, serializer())

            //SAPJEEDSR_Service, SAPJEEDSR_ServiceExt
        }
    }
    val xml = XML(module) {
        xmlDeclMode = XmlDeclMode.None
        autoPolymorphic = true
    }
    return xml
}

fun main() {
//    var cnt = 100000
//    while (cnt < 101336) {
//        val xmlSoap = Files.newBufferedReader(Paths.get("tmp/$cnt.xml")).readText()
//        println(cnt)
//        val obj = IntegratedConfiguration750ReadResponse.parse(xmlSoap)
//        cnt++
//    }
    // Очень простой клиент для проверки сериализатора
//    val auth = "Basic ?????????????????????????"
//    val host = "http://host:50000"
//    var con = URL(IntegratedConfigurationQueryRequest.getUrl750(host)).openConnection()
//    con.setRequestProperty("Authorization", auth)
//    con.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
//    con.doOutput = true
//    var writer = con.getOutputStream().writer()
//    writer.write(IntegratedConfigurationQueryRequest().composeSOAP())
//    writer.close()
//    con.connect()
//    var x = String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
//    IntegratedConfigurationQueryResponse.parse(x).IntegratedConfigurationID.forEach {
//        con = URL(IntegratedConfigurationReadRequest.getUrl750(host)).openConnection()
//        con.setRequestProperty("Authorization", auth)
//        con.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
//        con.doOutput = true
//        writer = con.getOutputStream().writer()
//        writer.write(IntegratedConfigurationReadRequest("User", mutableListOf(it)).composeSOAP())
//        writer.close()
//        con.connect()
//        x = String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
//        // Сохраняем как файл
//        val wr = Files.newBufferedWriter(Paths.get("tmp/${cnt++}.xml"))
//        wr.write(x)
//        wr.close()
//    }
}