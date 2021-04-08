@file:Suppress("unused", "EnumEntryName", "ClassName", "MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package k3

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Внутренний сериализатор, наружу не отдаётся
 */
private val jsonSerializer = Json {
    ignoreUnknownKeys = false
    isLenient = false
    encodeDefaults = true
    allowStructuredMapKeys = false
    prettyPrint = false
    coerceInputValues = false
    useArrayPolymorphism = false
}

enum class IFlowRuntimeStatus { RETRY, COMPLETED, PROCESSING, FAILED, DISCARDED, ESCALATED, CANCELLED }
enum class CpiLogLevel { NONE, INFO, DEBUG, TRACE }
enum class CpiDeployedStatus { STARTED, STARTING }
enum class IntegrationArtifactTypeEnum { INTEGRATION_FLOW, VALUE_MAPPING }
enum class LogFileTypeEnum { http, trace }
enum class NodeScopeEnum { worker }
enum class IntegrationPackageModeEnum { EDIT_ALLOWED }
enum class SecurityArtifactDescriptorStatusEnum { DEPLOYED }
enum class SecurityArtifactDescriptorTypeEnum { CREDENTIALS }
enum class DownloadContentTypeEnum { BUNDLE }
enum class StateEnum { DEPLOYED }

@Serializable
@SerialName("__metadata")
class __Metadata(
    val uri: String = "",
    val type: String = "",
    val id: String = "",
    val content_type: String = "",
    val media_src: String = "",
    val edit_media: String = "",
) {
    companion object {
        fun __deferred(): __Metadata = __Metadata()
    }
}

/** попытка 2021-03-20 сделать https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#content-based-polymorphic-deserialization
Пока его отбрасываем так как требует больше знаний и времени.
Делаем AdHoc запросы, или используем /api/v1/$metadata+Olingo2 или пишем свой ОДата-клиент на котлине

abstract class K3 {
abstract val __metadata: __Metadata
}

@Serializable
object K3Serializer : JsonContentPolymorphicSerializer<K3>(K3::class) {
fun a(element: JsonElement): String {
require(
element is JsonObject && element.containsKey("__metadata")
&& element["__metadata"] is JsonObject
)
val __metadata = element["__metadata"]
require(__metadata is JsonObject && __metadata.containsKey("type") && __metadata["type"] is JsonPrimitive)
val type = __metadata["type"] as JsonPrimitive
return type.content
}

override fun selectDeserializer(element: JsonElement) = when {
a(element) == "com.sap.hci.api.LogFileArchive" -> LogFileArchive.serializer()
a(element) == "com.sap.hci.api.ServiceEndpoint" -> ServiceEndpoint.serializer()
else -> K3.serializer()
}
}
@Serializable
class K3s(val d: K3Data) {
@Serializable
class K3Data(val results: List<K3>, val __next: String = "") {
}
}
@Serializable
class K3Data(val results: List<K3Data>, val __next: String = "")
@Serializable
class K3Wrapper(val d: K3Data)
 */

@Serializable
class __DefUri(
    val __deferred: __DeferredInt?,
) {
    @Serializable
    class __DeferredInt(val uri: String)

    fun getUri(): String {
        requireNotNull(__deferred)
        requireNotNull(__deferred.uri)
        return (__deferred.uri)
    }
}

@Serializable
class EntryPoint(
    val __metadata: __Metadata,
    val Name: String, //"UpdateServiceRequest",
    val Url: String, //"https://e45-iflmap.hcisbt.ru1.hana.ondemand.com/cxf/UpdateServiceRequest",
    val Type: String, //"PROD",
    val AdditionalInformation: String? = "", //null
)

@Serializable
class ApiDefinition(
    val __metadata: __Metadata,
    val Url: String, //"https://../Operations/api/WSDLDownload?artifactName=UpdateServiceRequest&servicePath=/UpdateServiceRequest&domainName=hcisbt.ru1.hana.ondemand.com&includePolicies=false",
    val Name: String, //"WSDL_POLICIES",
)

@Serializable
class ServiceEndpoint(
    val __metadata: __Metadata,     //com.sap.hci.api.ServiceEndpoint
    val Name: String,    // "Create follow up document from ServiceRequest in S4HANA from Sales Cloud",
    val Id: String,             // "UpdateServiceRequest$endpointAddress=UpdateServiceRequest",
    val Title: String,             // "UpdateServiceRequest",
    val Version: String, // "1.0.1",
    val Summary: String, // "UpdateServiceRequestInternalService in Sales Cloud",
    val Description: String, // "UpdateServiceRequestInternalService in Sales Cloud",
    val LastUpdated: String, // "/Date(1576581100242)/"
    val Protocol: String, // one of SOAP, REST, ODATAV2
    val EntryPoints: EntryPointsList, //   com.sap.hci.api.EntryPoint
    val ApiDefinitions: ApiDefinitionList, // com.sap.hci.api.Definition
) {
    @Serializable
    class EntryPointsList(
        val __deferred: __Metadata = __Metadata.__deferred(),
        val results: List<EntryPoint> = listOf(),
    )

    @Serializable
    class ApiDefinitionList(
        val __deferred: __Metadata = __Metadata.__deferred(),
        val results: List<ApiDefinition> = listOf(),
    )
}

//TODO -- подумать как избавиться от этого IntegrationArtifact в пользу рантайм артефакта
@Serializable
class IntegrationArtifact(
    val __metadata: __Metadata,
    val Id: String,
    val Name: String,
    val Type: IntegrationArtifactTypeEnum,
    val PackageId: String?,
    val PackageName: String?,
)

@Serializable
class MessageProcessingLog(
    val __metadata: __Metadata,
    val MessageGuid: String,
    val CorrelationId: String,
    val ApplicationMessageId: String?,
    val ApplicationMessageType: String?,
    val LogStart: String,       // /Date(1613521876190)/
    val LogEnd: String = "",    // /Date(1613521876190)/
    val Sender: String?,
    val Receiver: String?,
    val IntegrationFlowName: String,
    val Status: IFlowRuntimeStatus,
    val AlternateWebLink: String,
    val IntegrationArtifact: IntegrationArtifact,
    val LogLevel: CpiLogLevel,
    val CustomStatus: String?,
    val TransactionId: String?,
    val PreviousComponentName: String?,
    val LocalComponentName: String?,
    val OriginComponentName: String?,
    val CustomHeaderProperties: CustomHeaderPropertiesList,
    val MessageStoreEntries: __DefUri,
    val ErrorInformation: __DefUri,
    val AdapterAttributes: AdapterAttributesList,
    val Attachments: __DefUri,
    val Runs: __DefUri,
)

@Serializable
class CustomHeaderProperty(
    val __metadata: __Metadata,
    val Name: String, //"UpdateServiceRequest",
)

@Serializable
class CustomHeaderPropertiesList(
    val __deferred: __Metadata = __Metadata.__deferred(),
    val results: List<CustomHeaderProperty> = listOf(),
)

@Serializable
class AdapterAttribute(
    val __metadata: __Metadata,
    val Name: String, //"",
)

@Serializable
class AdapterAttributesList(
    val __deferred: __Metadata = __Metadata.__deferred(),
    val results: List<AdapterAttribute> = listOf(),
)


@Serializable
class MessageStoreEntry(
    val __metadata: __Metadata, //MessageStoreEntry
    val Id: String, //"sap-it-res:msg:j68386e77:4c52bfe9-0c0e-4595-b44e-434dd3ad9ae3",
    val MessageGuid: String, //"AGBVsuUVg4t6AL3duuy39rKvkl0w",
    val MessageStoreId: String, //"TemplateSender1",
    val TimeStamp: String, //"/Date(1616229093263)/",
    val HasAttachments: Boolean, //false,
    val Attachments: __DefUri,
    val Properties: __DefUri,
)

@Serializable
class ErrorInformation(
    val __metadata: __Metadata,
    val MessageGuid: String,
    val Type: String,
    val LastErrorModelStepId: String,
)

@Serializable
class Attachment(
    val __metadata: __Metadata,
    val Id: String,             // "sap-it-res:msg:j1f3f5ee1:daee9e6e-c068-4919-bf93-208daf5c1891"
    val MessageGuid: String,    // "AGAsY9TBVshDMOiDE4bn1oHJWvG2"
    val TimeStamp: String,      // "/Date(1616167332562)/"
    val Name: String,           // "Log - IDOC"
    val ContentType: String,    // "text/xml"
    val PayloadSize: String,     // "37542"
)

@Serializable
class IntegrationRuntimeArtifact(
    val __metadata: __Metadata, //com.sap.hci.api.IntegrationRuntimeArtifact
    val Id: String,             // "Create_follow_up_document_from_ServiceRequest_in_S4HANA_from_Sales_Cloud",
    val Version: String,        // "1.0.10"
    val Name: String,    // "Create follow up document from ServiceRequest in S4HANA from Sales Cloud",
    val Type: IntegrationArtifactTypeEnum,      // INTEGRATION_FLOW || VALUE_MAPPING
    val DeployedBy: String,           // "s0123456789"
    val DeployedOn: String,    // "/Date(1616167332562)/"
    val Status: CpiDeployedStatus,     // "37542"
    val ErrorInformation: __DefUri,
) {
    var integrationPackage: IntegrationPackage? = null
    val integrationDesigntimeArtifact: IntegrationDesigntimeArtifact? = null
    val errorInformation: ErrorInformation? = null
    var designtimeArtifact: IntegrationDesigntimeArtifact? = null
    var designtimeVMG: ValueMappingDesigntimeArtifact? = null
    fun orphan(): Boolean {
        return false
    }
}

@Serializable
class IntegrationPackage(
    val __metadata: __Metadata,     //com.sap.hci.api.IntegrationPackage
    val Id: String, // CommerceCloudwithS4HANAmodified
    val Name: String, // Commerce Cloud with S4HANA - modified
    val Description: String, // <p></p>
    val ShortText: String, // Modifications of standard iflows SAP Commerce Cloud Integration with S/4HANA
    val Version: String, // ""
    val Vendor: String,
    val PartnerContent: Boolean,
    val UpdateAvailable: Boolean,
    val Mode: IntegrationPackageModeEnum, //EDIT_ALLOWED
    val SupportedPlatform: String, //SAP Cloud Integration
    val ModifiedBy: String, // "S0014859446"
    val CreationDate: String, //"1563787275340" -- string millis
    val ModifiedDate: String?, //"1563787275340" -- string millis
    val CreatedBy: String, // "S0014859446"
    val Products: String?, //"",
    val Keywords: String?, //"",
    val Countries: String?, //"",
    val Industries: String?, //"",
    val LineOfBusiness: String?, //"",
    val PackageContent: String?, //null,
    val IntegrationDesigntimeArtifacts: __DefUri,
    val ValueMappingDesigntimeArtifacts: __DefUri,
    val CustomTags: __DefUri,
) {
    val ida: MutableList<IntegrationDesigntimeArtifact> = mutableListOf()
    val vmda: MutableList<ValueMappingDesigntimeArtifact> = mutableListOf()
}

@Serializable
class IntegrationPackages(val d: IntegrationPackagesD) {
    @Serializable
    class IntegrationPackagesD(val results: List<IntegrationPackage>, val __next: String = "")

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/IntegrationPackages"
        fun parse(payloadJson: String) = jsonSerializer.decodeFromString<IntegrationPackages>(payloadJson)
    }
}

@Serializable
class IntegrationDesigntimeArtifact(
    val __metadata: __Metadata,     //com.sap.hci.api.IntegrationDesigntimeArtifact
    val Id: String, //"Create-Delivery-From-SAP-Commerce-Cloud-To-SAP-S4HANA"
    val Version: String, //"1.0.7" or "Active"
    val PackageId: String, //"SALES05"
    val Name: String, //"Create-Delivery-From-SAP-Commerce-Cloud-To-SAP-S4HANA"
    val Description: String, //" "
    val Sender: String, //"Commerce"
    val Receiver: String, //"S4HANA"
    val ArtifactContent: String?, //null
    val Configurations: __DefUri,
    val Resources: __DefUri,
) {
    @Transient
    var integrationPackage: IntegrationPackage? = null
}

@Serializable
class IntegrationDesigntimeArtifacts(val d: IntegrationDesigntimeArtifactsData) {
    @Serializable
    class IntegrationDesigntimeArtifactsData(
        val results: List<IntegrationDesigntimeArtifact>, val __next: String = "",
    )

    companion object {
        // урла-то нет        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/IntegrationDesigntimeArtifacts"
//        fun parse(payloadJson: String) = jsonSerializer.decodeFromString<IntegrationDesigntimeArtifacts>(payloadJson)
        fun parse(payloadJson: String, pack: IntegrationPackage?): IntegrationDesigntimeArtifacts {
            val xs = jsonSerializer.decodeFromString<IntegrationDesigntimeArtifacts>(payloadJson)
            if (pack != null) xs.d.results.forEach { it.integrationPackage = pack }
            return xs
        }
    }
}

@Serializable
class ValueMappingDesigntimeArtifact(
    val __metadata: __Metadata,     //com.sap.hci.api.ValueMappingDesigntimeArtifact
    val Id: String, //"OtherPartyMapping",
    val Version: String, //"1.0.2",
    val PackageId: String, //"C4Custom",
    val Name: String, //"OtherPartyMapping",
    val Description: String?, //" ",
    val ArtifactContent: String?, //null,
    val ValMapSchema: __DefUri,
) {
    @Transient
    var integrationPackage: IntegrationPackage? = null
}

@Serializable
class ValueMappingDesigntimeArtifacts(val d: ValueMappingDesigntimeArtifactsData) {
    @Serializable
    class ValueMappingDesigntimeArtifactsData(
        val results: List<ValueMappingDesigntimeArtifact>,
        val __next: String = "",
    )

    companion object {
        // урла-то нет       fun getUrl(hostTmn: String) = "$hostTmn/api/v1/ValueMappingDesigntimeArtifacts"
        fun parse(payloadJson: String, pack: IntegrationPackage?): ValueMappingDesigntimeArtifacts {
            val xs = jsonSerializer.decodeFromString<ValueMappingDesigntimeArtifacts>(payloadJson)
            if (pack != null) xs.d.results.forEach { it.integrationPackage = pack }
            return xs
        }
    }
}

@Serializable
class LogFile(
    val __metadata: __Metadata,     //com.sap.hci.api.LogFile
    val Name: String, //"http_access_2f39eb6_2021-03-13.log",
    val Application: String, //"e4500iflmap",
    val LastModified: String, //"/Date(1615679941000)/",
    val ContentType: String, //"application/gzip",
    val LogFileType: LogFileTypeEnum, //"http",
    val NodeScope: NodeScopeEnum, //"worker",
    val Size: String, //"7988"
)

@Serializable
class LogFileArchive(
    val __metadata: __Metadata,     //com.sap.hci.api.LogFileArchive
    val Scope: String,    // "all",
    val LogFileType: LogFileTypeEnum,             // "UpdateServiceRequest$endpointAddress=UpdateServiceRequest",
    val NodeScope: NodeScopeEnum,             // "UpdateServiceRequest",
    val ContentType: String, // com.sap.hci.api.Definition
)

@Serializable
class SecurityArtifactDescriptor(
    val __metadata: __Metadata,     //com.sap.hci.api.SecurityArtifactDescriptor
    val Type: SecurityArtifactDescriptorTypeEnum, //"CREDENTIALS",
    val DeployedBy: String, //"S0020379160",
    val DeployedOn: String, //"/Date(1563279368554)/",
    val Status: SecurityArtifactDescriptorStatusEnum, //"DEPLOYED"
)

@Serializable
class UserCredential(
    val __metadata: __Metadata,     //com.sap.hci.api.UserCredential
    val Name: String, //"SalesCloudCredential",
    val Kind: String, //"default",
    val Description: String, //"ServiceRequestCollection",
    val User: String, //"CLOUDCOMMERCECLAIM",
    val Password: String?, //null,
    val CompanyId: String?, //null,
    val SecurityArtifactDescriptor: SecurityArtifactDescriptor,
)


@Serializable
class Configuration(
    val __metadata: __Metadata,     //com.sap.hci.api.Configuration
    val ParameterKey: String,       //"BusinessSystem"
    val ParameterValue: String,     //"BS_SCPI_Q"
    val DataType: String,            //"xsd:string"
)

@Serializable
class Resource(
    val __metadata: __Metadata,     //com.sap.hci.api.Resource
    val Name: String, //"logMessage.groovy"
    val ResourceType: String, //"groovy"
    val ReferencedResourceType: String?, //null
    val ResourceContent: String?, //null
)


// ------------------------------------------------------------------------------------
@Serializable
class MplDetailCommand(
    val messageGuid: String, //"AGBR51C6kL4uQkOorOEXuA3fDhAn"
    val mplData: String = "", //long text
    val lastError: String? = null, //"com.sap.gateway.core.ip.component.odata.exception.OsciException: Internal Server Error : 500 : HTTP/1.1 @ line 19 in ThrowException.groovy"
    val mplAttachments: List<MplAttachment2>,
) {
    @Serializable
    class MplAttachment2(
        val uri: String, //"sap-it-res:msg:j1f3f5ee1:ce8909db-4d99-47c2-a95c-2015a122aa20",
        val attachmentName: String, //"Log - Message",
        val contentType: String, //"text/xml",
        val timestamp: String, //"Mar 17, 2021 11:26:08 AM",
        val payloadSize: Long, //3408
    ) {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/MessageProcessingLogAttachments('$uri')/\$value"
    }

    companion object {
        fun getUrl(hostTmn: String, messageGuid: String) =
            "$hostTmn/Operations/com.sap.it.op.tmn.commands.dashboard.webui.MplDetailCommand?messageGuid=$messageGuid"
// 2nd variant - "$host/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.MplDetailCommand?messageGuid=$messageGuid"
    }
}

@Serializable
class OPTag(val name: String, val value: String)

//com.sap.it.nm.commands.deploy.DownloadContentCommand
@Serializable
class OPDownloadContentCommand(
    val artifacts: List<DownloadContent>, //"AGBR51C6kL4uQkOorOEXuA3fDhAn"
) {
    @Serializable
    class DownloadContent(
        val content: ByteArray,
        val id: String, //"6ef77148-d236-41a3-b9ea-e6f703e7f774",
        val name: String, //"Replicate Claims Item from Sales RU to Sales EU",
        val tags: List<OPTag>,
        val type: DownloadContentTypeEnum,
    )
}

// /itspaces/api/1.0/configurations
@Serializable
class OPConfigurationKeyValue(val key: String, val value: JsonElement) {
    companion object {
        val goodKeys = listOf("tenantId", "buildNumber", "nodeType", "tenantName", "webdavSupported")
        fun getUrl(hostTmn: String) =
            "$hostTmn/itspaces/api/1.0/configurations"

        /**
         * Разбираем весь список
         */
        fun parseList(payloadJson: String): List<OPConfigurationKeyValue> = jsonSerializer.decodeFromString(payloadJson)

        /**
         * Только интересные штуки, а то там много чепухи для браузера
         */
        fun getMap(payloadJson: String): Map<String, String> {
            val lst: List<OPConfigurationKeyValue> = jsonSerializer.decodeFromString(payloadJson)
            return lst.mapNotNull {
                if (goodKeys.contains(it.key))
                    Pair(it.key, it.value.jsonPrimitive.content)
                else
                    null
            }.toMap()
        }
    }
}
//class OPCpiParams(val tenantId: String, val buildNumber: String, val tenantName:String)

@Serializable
class OPIntegrationComponentsListCommand(val artifactInformations: List<OPArtifactInformation>) {
    @Serializable
    class OPArtifactInformation(
        val id: String, //"6ab549ff-888e-40f4-ac2c-7fa39de32beb"
        val name: String, //"VM_UOM"
        val symbolicName: String, //"VM_UOM"
        val type: String, //"VALUE_MAPPING"
        val version: String, //"1.0.3"
        val deployedOn: String, //"Aug 5, 2019 1:02:58 PM"
        val deployedBy: String, //"S0014859446"
        val deployState: String, //"DEPLOYED"
        val tenantId: String, //"j68386e77"
        val semanticState: String, //"STARTED"
        val nodeType: String, //"IFLMAP"
        val tags: List<OPTag>,
    )

    companion object {
        fun getUrl(hostTmn: String) =
            "$hostTmn/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand"
//            "$host/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand"
    }
}

@Serializable
class OPGetNodesCommand(val nodes: List<OPGetNodesCommandNode>) {
    @Serializable
    class OPGetNodesCommandNode(
        val baseUrl: String,    //"https://vsa8281954:8041/"
        val clusterName: String, //"avtru1cpie.gbt7002.e450000"
        val components: List<OPGetNodesCommandNodeComponent>,
        val deployedArtifacts: List<OPGetNodesCommandNodeDeployedArtifact>,
        val dispatcherUris: List<String>,
        val id: String,         //"9df44d42efe7323643c55dc46de1fc43fcdb0972"
        val launchTime: Long,   //1616219833118
        val nodeVmSize: String, //"PRO"
        val profileId: String,  //"7db286f2-8517-3015-b7a3-9942a0189bad"
        val profileName: String, //"iflmap"
        val version: JsonElement, //  "version": {val major:String, val minor:String, val micro:String, "qualifier": ""
        val shipmentType: String, //"PRODUCTION"
        val stamp: Long,        //1616488220650
        val state: String,      //"LIVE"
        val tags: List<OPTag>,
        val tenant: JsonElement,
        val nodeType: String,   //"IFLMAP"
        val application: String, //"e450000iflmap"
        val account: String,    //"avtru1cpie"
    ) {
        @Serializable
        class OPGetNodesCommandNodeComponent(
            val desc: String, //"Monitor for Aries Subsystem Service Availability"
            val name: String, //"Subsystem Service"
            val artifactId: String = "",    //
            val state: String, //"STARTED"
            val tags: List<OPTag>, //[]
            val type: String, //"ESSENTIAL_SERVICE"
            val version: String = "", //"5.19.6"
            //TODO -- пока stateMessage неинтересно, нужен контрпример если прямо насущно
            val stateMessage: JsonElement = JsonNull, // OPGetNodesCommandNodeComponentStateMessage?, //Map<String, OPGetNodesCommandNodeComponentStateMessage>?,
            val restartable: Boolean, //false
            val syncState: String, //"NOT_APPLICABLE"
            val adapterPollInfos: JsonArray,   //TODO -- нужен живой пример для уточнения типа
        )

        @Serializable
        class OPGetNodesCommandNodeDeployedArtifact(
            val id: String, //"02a15918-e7fd-4af0-ae51-5658534d83f1"
            val name: String, //"Custom Confirmation from Sales to MDG"
            val symbolicName: String, //"Custom_Confirmation_from_Sales_to_MDG"
            val nodeType: String, //"IFLMAP"
            val state: String, //"DEPLOYED"
            val tags: List<OPTag>,
            val tenantId: String, //"j68386e77"
            val type: String, //"BUNDLE"
            val version: String, //"1.0.6"
            val deployedBy: String, //"S0020379160"
            val deployedOn: String, //"Mar 20, 2021 6:00:03 AM"
            val description: String = "", //" "
            val linkedComponentType: String = "", //"INTEGRATION_FLOW"
        )
    }

    companion object {
        fun getUrl(hostTmn: String) =
            "$hostTmn/Operations/com.sap.it.nm.commands.node.GetNodesCommand"
    }
}

@Serializable
class WebIFlow(
    val id: String, //"a949d19c-585c-4d32-9a73-36513cb01393",
    val bundleName: String, //"Update-Delivery-From-SAP-Commerce-Cloud-To-SAP-S4HANA",
    val state: StateEnum, //"DEPLOYED",
    val bundleVersion: String, //"1.0.6",
    val date: String, //"Fri Mar 13 12:44:48 UTC 2020",
    val type: IntegrationArtifactTypeEnum, //"INTEGRATION_FLOW"
) {
    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/itspaces/api/1.0/iflows/"
    }
}

@Serializable
class WebIFlowSingle(
    val version: String, //"1.34",
    val bpmnModel: BpmnModel,
    val propertyViewModel: JsonObject,
    val editActionModel: JsonObject,
    val listOfExternalizedPropertiesModel: JsonArray,
    val galileiModel: JsonObject,
) {
    @Serializable
    class BpmnModel(
        val iflowName: String, //"Replicate-Shipment-From-SAP-Commerce-Cloud-To-SAP-S4HANA",
        val __type: String, //"iflow",
        val orientation: String, //"Vertical",
        val isDirty: Boolean, //false,
        val shapes: JsonArray,
        val connectors: JsonArray,
    )

    @Serializable
    class Resource(
        val resourceName: String, //"LogMessage",
        val resourceLocation: String, //"script",
        val resourceType: String, //"groovy",
        val resourceExtension: String, //"groovy",
        val resourceCategory: String, //"{com.sap.it.spc.myproj.i18n>CATEGORY_SCRIPTS}"
    )

    companion object {
        fun getUrl(hostTmn: String, messageGuid: String) = "$hostTmn/itspaces/api/1.0/iflows/$messageGuid"
        fun getUrlResource(hostTmn: String, messageGuid: String) =
            "$hostTmn/itspaces/api/1.0/iflows/$messageGuid/resource/"
    }
}

// ------------------------------------------------------------------------------------
@Serializable
class MessageProcessingLogs(val d: MessageProcessingLogsData) {
    @Serializable
    class MessageProcessingLogsData(
        val results: List<MessageProcessingLog> = listOf(),
        val __next: String = "",
    )

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/MessageProcessingLogs"
    }
}

@Serializable
class MessageProcessingLogSingle(val d: MessageProcessingLog) {
    companion object {
        fun getUrl(hostTmn: String, messageGuid: String, expand: Boolean = true) = if (expand)
            "$hostTmn/api/v1/MessageProcessingLogs('$messageGuid')?\$expand=CustomHeaderProperties,AdapterAttributes"
        else
            "$hostTmn/api/v1/MessageProcessingLogs('$messageGuid')"
    }
}


@Serializable
class MPLErrorInformation(val d: ErrorInformation)

@Serializable
class MPLAttachments(val d: MPLAttachmentsData) {
    @Serializable
    class MPLAttachmentsData(val results: List<Attachment>, val __next: String = "")
    companion object {
        fun getUrl(hostTmn: String, messageGuid: String) =
            "$hostTmn/api/v1/MessageProcessingLogs('$messageGuid')/Attachments"
    }
}

@Serializable
class IntegrationRuntimeArtifacts(val d: IntegrationRuntimeArtifactsData) {
    @Serializable
    class IntegrationRuntimeArtifactsData(val results: List<IntegrationRuntimeArtifact>, val __next: String = "")
    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/IntegrationRuntimeArtifacts"
        fun parse(payloadJson: String) = jsonSerializer.decodeFromString<IntegrationRuntimeArtifacts>(payloadJson)
    }
}

@Serializable
class ServiceEndpoints(val d: ServiceEndpointsData) {
    @Serializable
    class ServiceEndpointsData(val results: List<ServiceEndpoint>, val __next: String = "")
    companion object {
        fun getUrl(hostTmn: String, expand: Boolean) = if (expand)
            "$hostTmn/api/v1/ServiceEndpoints?\$expand=EntryPoints,ApiDefinitions"
        else
            "$hostTmn/api/v1/ServiceEndpoints"

        fun parse(payloadJson: String) = jsonSerializer.decodeFromString<ServiceEndpoints>(payloadJson)
    }
}

@Serializable
class LogFiles(val d: LogFilesData) {
    @Serializable
    class LogFilesData(
        val results: List<LogFile>, val __next: String = "",
    )

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/LogFiles"
    }
}

@Serializable
class LogFileArchives(val d: LogFileArchivesData) {
    @Serializable
    class LogFileArchivesData(val results: List<LogFileArchive>, val __next: String = "")
    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/LogFileArchives"
    }
}

@Serializable
class UserCredentials(val d: UserCredentialsData) {
    @Serializable
    class UserCredentialsData(
        val results: List<UserCredential>, val __next: String = "",
    )

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/UserCredentials"
    }
}

@Serializable
class MessageStoreEntries(val d: MessageStoreEntriesData) {
    @Serializable
    class MessageStoreEntriesData(
        val results: List<MessageStoreEntry>, val __next: String = "",
    )

    companion object {
        fun getUrl(hostTmn: String, messageGuid: String) =
            "$hostTmn/api/v1/MessageProcessingLogs('$messageGuid')/MessageStoreEntries"
    }
}


@Serializable
class Configurations(val d: ConfigurationsData) {
    @Serializable
    class ConfigurationsData(
        val results: List<Configuration>, val __next: String = "",
    )

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/Configurations"
    }
}

@Serializable
class Resources(val d: ResourcesData) {
    @Serializable
    class ResourcesData(
        val results: List<Resource>, val __next: String = "",
    )

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/api/v1/Resources"
    }
}


// /itspaces/odata/1.0/workspace.svc/
// https://{{tmn}}/itspaces/odata/1.0/workspace.svc/ContentPackages
// близкий аналог -- https://{{tmn}}/api/v1/IntegrationPackages
@Serializable
class ContentPackages(val d: ContentPackagesD) {
    @Serializable
    class ContentPackagesD(val results: List<ContentPackage>)

    @Serializable
    class ContentPackage(
        val __metadata: __Metadata,
        val TechnicalName: String, //"SALES04",
        val DisplayName: String, //"SALES.04 - Secure storage",
        val ShortText: String, //"Secure storage (SALES.04)",
        val reg_id: String, //"0e24e0ed565f49f49d9a35f49d44d9f1",
        val Featured: String? = null, //null,
        val Scope: String? = null, //null,
        val Description: String = "", //"<p>123</p>",
        val Version: String? = null, //null,
        val Category: String? = null, //"Integration",
        val Mode: IntegrationPackageModeEnum, //"EDIT_ALLOWED",
        val Vendor: String, //"vendor.com",
        val OrgName: String? = null, //null,
        val SupportedPlatforms: String, //"SAP HANA Cloud Integration",
        val Products: String? = null, //null,
        val Industries: String? = null, //null,
        val LineOfBusiness: String? = null, //null,
        val Keywords: String? = null, //null,
        val Countries: String? = null, //null,
        val AvgRating: String? = null, //"0.0",
        val RatingCount: String? = null, //"0",
        val PublishedAt: String? = null, //null,
        val PublishedBy: String? = null, //null,
        val CreatedAt: String, //"/Date(1554276811896)/",
        val CreatedBy: String, //"S0014859446",
        val ModifiedAt: String? = null, //"/Date(1556286825646)/",
        val ModifiedBy: String? = null, //"S0020379160",
        val PartnerContent: String? = null, //null,
        val CertifiedBySap: String? = null, //null,
        val AdditionalAttributes: String? = null, //null,
    )

    companion object {
        fun getUrl(hostTmn: String) = "$hostTmn/itspaces/odata/1.0/workspace.svc/ContentPackages"
    }
}
