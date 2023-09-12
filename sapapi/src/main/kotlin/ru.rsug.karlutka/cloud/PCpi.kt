package ru.rsug.karlutka.cloud

import kotlinx.serialization.*
//TODO подумать и переписать с io.ktor:ktor-serialization-kotlinx-json-jvm
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.decodeFromJsonElement
import kotlinx.serialization.modules.EmptySerializersModule
import java.io.InputStream

// https://help.sap.com/docs/cloud-integration/sap-cloud-integration/message-processing-logs
// https://api.sap.com/api/MessageProcessingLogs/overview
class PCpi {
    @Serializable
    class Error(
        val code: String,
        val message: Message
    ) {
        @Serializable
        class Message(
            val lang: String,
            val value: String
        )
    }

    @Serializable
    class __Metadata(
        val type: String,
        val id: String? = null,
        val uri: String? = null,
        val content_type: String? = null,
        val media_src: String? = null,
        val edit_media: String? = null
    )

    @Serializable
    class __Deferred<T>(
        val __deferred: Uri? = null,
        val results: List<T> = listOf()
    )

    @Serializable
    class Uri(
        val uri: String
    )

    @Serializable
    open class ODataJson(
        val __metadata: __Metadata? = null
    )

    @Serializable
    class Attachment(
        val Id: String, //"Id": "sap-it-res:msg:j1f3f5ee1:5fe171ef-12fe-49a6-9cad-1fe572c5f1e5",
        val MessageGuid: String, //"MessageGuid": "AGAsY9TBVshDMOiDE4bn1oHJWvG2",
        val TimeStamp: String, //TODO переделать на Jsondate.serializer() "/Date(1563279368554)/",
        val Name: String, //"Log - msgHeader"
        val ContentType: String, //"text/xml"
        val PayloadSize: String, // "40176"
    ) : ODataJson()

    @Serializable
    class SecurityArtifactDescriptor(
        val Type: String, //"CREDENTIALS",
        val DeployedBy: String, //"S0020379160",
        val DeployedOn: String, //TODO переделать на Jsondate.serializer() "/Date(1563279368554)/",
        val Status: String, //"DEPLOYED"
    ) : ODataJson()

    @Serializable
    class UserCredential(
        val Name: String, //"SalesCloudCredential",
        val Kind: String, //"default",
        val Description: String, //"ServiceRequestCollection",
        val User: String, //"CLOUDCOMMERCECLAIM",
        val Password: String?, //null,
        val CompanyId: String?, //null,
        val SecurityArtifactDescriptor: SecurityArtifactDescriptor,
    ) : ODataJson()

    @Serializable
    class DataStore(
        val DataStoreName: String,
        val IntegrationFlow: String,
        val Type: String,
        val Visibility: String? = null,
        val NumberOfMessages: Long? = null,
        val NumberOfOverdueMessages: Long? = null,
        val Entries: __Deferred<ODataJson>? = null //NavigationProperty Relationship="com.sap.hci.api.DataStore_2_r_Entries" FromRole="DataStore" ToRole="r_Entries"
    ) : ODataJson()

    @Serializable
    class DataStoreEntry(
        val Id: String,  //SAPCpiOutboundOrder_10496001A_ZTD
        val DataStoreName: String, //HYBRISCOMMERCE
        val IntegrationFlow: String?, //
        val Type: String = "",
        val Status: String, //Overdue
        val MessageId: String? = null,
        val DueAt: String, //TODO /Date(1654433933006)/
        val CreatedAt: String, //TODO /Date(1654261133006)/
        val RetainUntil: String, //TODO /Date(1662037133006)/
    ) : ODataJson()

    @Serializable
    class IntegrationPackage(
        val Id: String, // CommerceCloudwithS4HANAmodified
        val Name: String, // Commerce Cloud with S4HANA - modified
        val ResourceId: String, //ef44d444d648433a8da21a3bfe3ba247
        val Description: String, // <p></p>
        val ShortText: String, // траляля
        val Version: String, // ""
        val Vendor: String,
        val PartnerContent: Boolean,
        val UpdateAvailable: Boolean,
        val Mode: String, //EDIT_ALLOWED
        val SupportedPlatform: String, //SAP Cloud Integration
        val ModifiedBy: String, // "S00000000000"
        val CreationDate: String, //"1563787275340" -- string millis
        val ModifiedDate: String?, //"1563787275340" -- string millis
        val CreatedBy: String, // "S00000000000"
        val Products: String?, //"",
        val Keywords: String?, //"",
        val Countries: String?, //"",
        val Industries: String?, //"",
        val LineOfBusiness: String?, //"",
        val PackageContent: String?, //null,
        val IntegrationDesigntimeArtifacts: __Deferred<ODataJson>,
        val ValueMappingDesigntimeArtifacts: __Deferred<ODataJson>,
        val MessageMappingDesigntimeArtifacts: __Deferred<ODataJson>,
        val CustomTags: __Deferred<ODataJson>,
    ) : ODataJson()

    @Serializable
    class IntegrationArtifact(
        val Id: String?,                // при чтении старых потоков почему-то может стрелять нулями
        val Name: String?,
        val Type: String?,
        val PackageId: String?,
        val PackageName: String?,
    ) : ODataJson()

    @Serializable
    class IntegrationDesigntimeArtifact(
        val Id: String, //"Create-Delivery-From-SAP-Commerce-Cloud-To-SAP-S4HANA"
        val Version: String, //"1.0.7" or "Active"
        val PackageId: String, //"SALES05"
        val Name: String, //"Create-Delivery-From-SAP-Commerce-Cloud-To-SAP-S4HANA"
        val Description: String, //" "
        val Sender: String, //"Commerce"
        val Receiver: String, //"S4HANA"
        val ArtifactContent: String?, //null
        val Configurations: __Deferred<ODataJson>,
        val Resources: __Deferred<ODataJson>,
    ) : ODataJson()

    @Serializable
    class ValueMappingDesigntimeArtifact(
        val Id: String, //"OtherPartyMapping",
        val Version: String, //"1.0.2",
        val PackageId: String, //"C4Custom",
        val Name: String, //"OtherPartyMapping",
        val Description: String?, //" ",
        val ArtifactContent: String?, //null,
        val ValMapSchema: __Deferred<ODataJson>,
    ) : ODataJson()

    @Serializable
    class ServiceEndpoint(
        val Name: String,    // "Create follow up document from ServiceRequest in S4HANA from Sales Cloud",
        val Id: String,             // "UpdateServiceRequest$endpointAddress=UpdateServiceRequest",
        val Title: String,             // "UpdateServiceRequest",
        val Version: String, // "1.0.1",
        val Summary: String, // "UpdateServiceRequestInternalService in Sales Cloud",
        val Description: String, // "UpdateServiceRequestInternalService in Sales Cloud",
        val LastUpdated: String, // "/Date(1576581100242)/"
        val Protocol: String, // one of SOAP, REST, ODATAV2
        val EntryPoints: __Deferred<EntryPoint>, //   com.sap.hci.api.EntryPoint
        val ApiDefinitions: __Deferred<ApiDefinition>, // com.sap.hci.api.Definition
    ) : ODataJson()

    @Serializable
    class EntryPoint(
        val Name: String, //"UpdateServiceRequest",
        val Url: String, //"https://e45-iflmap.hcisbt.ru1.hana.ondemand.com/cxf/UpdateServiceRequest",
        val Type: String, //"PROD",
        val AdditionalInformation: String? = "", //null
    ) : ODataJson()

    @Serializable
    class ApiDefinition(
        val Url: String, //"https://../Operations/api/WSDLDownload?artifactName=UpdateServiceRequest&servicePath=/UpdateServiceRequest&domainName=hcisbt.ru1.hana.ondemand.com&includePolicies=false",
        val Name: String, //"WSDL_POLICIES",
    ) : ODataJson()

    @Serializable
    class MessageProcessingLog(
        val MessageGuid: String,
        val CorrelationId: String? = null,
        val ApplicationMessageId: String? = null,
        val ApplicationMessageType: String? = null,
        val LogStart: String? = null,       // /Date(1613521876190)/
        val LogEnd: String? = null,    // /Date(1613521876190)/
        val Sender: String? = null,
        val Receiver: String? = null,
        val IntegrationFlowName: String? = null,
        val Status: String,             //PROCESSING
        val AlternateWebLink: String,
        val IntegrationArtifact: IntegrationArtifact,
        val LogLevel: String? = null,           //INFO
        val CustomStatus: String? = null,      //COMPLETED

        val ArchivingStatus: String? = null,   //NOT_RELEVANT
        val ArchivingSenderChannelMessages: Boolean,
        val ArchivingReceiverChannelMessages: Boolean,
        val ArchivingLogAttachments: Boolean,
        val ArchivingPersistedMessages: Boolean,

        val TransactionId: String? = null,
        val PreviousComponentName: String? = null,
        val LocalComponentName: String? = null,
        val OriginComponentName: String? = null,
        val CustomHeaderProperties: __Deferred<MessageProcessingLogCustomHeaderProperties>?,
        val MessageStoreEntries: __Deferred<ODataJson>?,
        val ErrorInformation: __Deferred<ODataJson>?,
        val AdapterAttributes: __Deferred<ODataJson>?,
        val Attachments: __Deferred<ODataJson>?,
        val Runs: __Deferred<ODataJson>?,
    ) : ODataJson()

    @Serializable
    class MessageProcessingLogCustomHeaderProperties(
        val Id: String,
        val Name: String,
        val Value: String,
        val Log: __Deferred<ODataJson>
    ) : ODataJson()

    @Serializable
    class LogFile(
        val Name: String,               //"http_access_2f39eb6_2021-03-13.log",
        val Application: String,        //"e4500iflmap",
        val LastModified: String,       //"/Date(1615679941000)/",
        val ContentType: String,        //"application/gzip",
        val LogFileType: String,        //"http",
        val NodeScope: String,          //"worker",
        val Size: String,               //"7988"
    ) : ODataJson()

    @Serializable
    class LogFileArchive(
        val Scope: String,              // "all",
        val LogFileType: String,        // "UpdateServiceRequest$endpointAddress=UpdateServiceRequest",
        val NodeScope: String,          // "UpdateServiceRequest",
        val ContentType: String,        // com.sap.hci.api.Definition
    ) : ODataJson()

    @Serializable
    class ODataJsonRoot<T>(val d: OData_T<T>)

    @Serializable
    class OData_T<T>(
        // Выдаётся внутри d
        val __count: String? = null,    // "__count": "1092770" - при $inlinecount=allpages
        val results: List<T>,
        val __next: String? = null      // если есть куда листать дальше
    )

    companion object {

//        val Json1 = Json(JsonConfiguration(), EmptySerializersModule())
//        val __metas = mapOf(
//            // закомментированных нет на верхнем уровне.
//            // Нас интересует полиморфизм верхнего уровня, но здесь для поиска пусть будет
////            "com.sap.hci.api.SecurityArtifactDescriptor" to SecurityArtifactDescriptor::class,
////            "com.sap.hci.api.IntegrationArtifact" to IntegrationArtifact::class,
////            "com.sap.hci.api.MessageProcessingLogCustomHeaderProperty" to MessageProcessingLogCustomHeaderProperty::class,
//            "com.sap.hci.api.UserCredential" to UserCredential::class,
//            "com.sap.hci.api.DataStore" to DataStore::class,
//            "com.sap.hci.api.DataStoreEntry" to DataStoreEntry::class,
//            "com.sap.hci.api.IntegrationPackage" to IntegrationPackage::class,
//            "com.sap.hci.api.IntegrationDesigntimeArtifact" to IntegrationDesigntimeArtifact::class,
//            "com.sap.hci.api.ValueMappingDesigntimeArtifact" to ValueMappingDesigntimeArtifact::class,
//            "com.sap.hci.api.ServiceEndpoint" to ServiceEndpoint::class,
//            "com.sap.hci.api.MessageProcessingLog" to MessageProcessingLog::class,
//            "com.sap.hci.api.LogFile" to LogFile::class,
//            "com.sap.hci.api.LogFileArchive" to LogFileArchive::class,
//        )

//        @OptIn(InternalSerializationApi::class)
//        fun serializerFromMetadata(m: JsonElement): KSerializer<out ODataJson> {
//            val j = m.jsonObject["__metadata"]
//            requireNotNull(j) { "Элемент должен содержать __metadata" }
//            val type = (j.jsonObject["type"]!! as JsonPrimitive).content
//            val kl = __metas[type]
//            requireNotNull(kl) { "Не найден сериализатор для $type" }
//            return kl.serializer()
//        }

//        @Serializable
//        class ODataJsonList(
//            val __count: String? = null,    // "__count": "1092770"
//            val results: List<JsonElement>,
//            val __next: String? = null
//        )

//        @OptIn(ExperimentalSerializationApi::class)
//        inline fun <reified T> parse(sjson: InputStream): Triple<List<T>, String?, Long?> {
//            val d = Json.decodeFromStream<ODataJsonRoot>(sjson)
//            val results = d.d.results
//            val out = mutableListOf<T>()
//            var ix = 0
//            results.forEach { j ->
//                println(ix++)
//                val kl = serializerFromMetadata(j)
//                val s = decodeFromJsonElement(kl, j)
//                require(s is T) { "запрошен разбор типа ${T::class} но обнаружен ${s::class}" }
//                out.add(s)
//            }
//            return Triple(out, d.d.__next, 1)
//        }

//        inline fun <reified T> parse(sjson: String): Pair<List<T>, String?> {
//            val d = Json.decodeFromString<ODataJsonRoot>(sjson)
//            val results = d.d.results
//            val out = mutableListOf<T>()
//            results.forEach { j ->
//                val s = decodeFromJsonElement(serializerFromMetadata(j), j)
//                require(s is T) { "запрошен разбор типа ${T::class} но обнаружен ${s::class}" }
//                out.add(s)
//            }
//            return Pair(out, d.d.__next)
//        }
//
//        inline fun <reified T> parseSingle(sjson: String): T {
//            val src = Json.decodeFromString<JsonObject>(sjson)
//            val j = src["d"]!!
//            val s = decodeFromJsonElement(serializerFromMetadata(j), j)
//            require(s is T) { "запрошен разбор типа ${T::class} но обнаружен ${s::class}" }
//            return s
//        }

        @Serializable
        private class X(val error: Error)

        fun parseError(sjson: String): Error {
            return Json.decodeFromString<X>(sjson).error
        }

        //TODO разобраться как сделать не-inline вариант этой функции, через регистрацию стратегии
        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T:ODataJson> parseStream(x: InputStream): ODataJsonRoot<T> {
            val y = Json.decodeFromStream<ODataJsonRoot<T>>(x)
            return y
        }
    }
}