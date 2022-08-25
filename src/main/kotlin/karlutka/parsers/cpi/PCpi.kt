package karlutka.parsers.cpi

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class PCpi {
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
    open class ODataJson(
        val __metadata: __Metadata? = null
    )

    @Serializable
    data class SecurityArtifactDescriptor(
        val Type: String, //"CREDENTIALS",
        val DeployedBy: String, //"S0020379160",
        val DeployedOn: String, //TODO переделать на Jsondate.serializer() "/Date(1563279368554)/",
        val Status: String, //"DEPLOYED"
    ) : ODataJson()

    @Serializable
    data class UserCredential(
        val Name: String, //"SalesCloudCredential",
        val Kind: String, //"default",
        val Description: String, //"ServiceRequestCollection",
        val User: String, //"CLOUDCOMMERCECLAIM",
        val Password: String?, //null,
        val CompanyId: String?, //null,
        val SecurityArtifactDescriptor: SecurityArtifactDescriptor,
    ) : ODataJson()

    @Serializable
    data class DataStoreEntry(
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
    class ODataJsonRoot(val d: ODataJsonD)

    @Serializable
    class ODataJsonD(
        val results: List<JsonElement>,
        val __next: String? = null
    )

    companion object {
        val __metas = mapOf(
            "com.sap.hci.api.SecurityArtifactDescriptor" to SecurityArtifactDescriptor::class,
            "com.sap.hci.api.UserCredential" to UserCredential::class,
            "com.sap.hci.api.DataStoreEntry" to DataStoreEntry::class,
        )

        @OptIn(InternalSerializationApi::class)
        fun serializerFromMetadata(m: JsonElement): KSerializer<out ODataJson> {
            val j = m.jsonObject["__metadata"]
            requireNotNull(j) { "Элемент должен содержать __metadata" }
            val type = (j.jsonObject["type"]!! as JsonPrimitive).content
            val kl = __metas[type]
            requireNotNull(kl) { "Не найден сериализатор для $type" }
            return kl.serializer()
        }

        fun <T> parse(sjson: String): Pair<List<T>, String?> {
            val d = Json.decodeFromString<ODataJsonRoot>(sjson)
            val results = d.d.results
            val out = mutableListOf<ODataJson>()
            results.forEach { j ->
                val s = decodeFromJsonElement(serializerFromMetadata(j), j)
                requireNotNull(s.__metadata)
                out.add(s)
            }
            return Pair(out as List<T>, d.d.__next)
        }
    }
}