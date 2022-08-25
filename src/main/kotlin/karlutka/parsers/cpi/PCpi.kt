package karlutka.parsers.cpi

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class PCpi {
    @Serializable
    data class __Metadata(
        val type: String,
        val id: String? = null,
        val uri: String? = null,
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
    class ODataJsonRoot(val d: ODataJsonD)

    @Serializable
    class ODataJsonD(val results: List<JsonElement>)

    companion object {
        val __metas = mapOf(
            "com.sap.hci.api.SecurityArtifactDescriptor" to SecurityArtifactDescriptor::class,
            "com.sap.hci.api.UserCredential" to UserCredential::class
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

        fun <T> parse(sjson: String): List<T> {
            val d = Json.decodeFromString<ODataJsonRoot>(sjson)
            val results = d.d.results
            val out = mutableListOf<ODataJson>()
            results.forEach { j ->
                val s = decodeFromJsonElement(serializerFromMetadata(j), j)
                requireNotNull(s.__metadata)
                out.add(s)
            }
            return out as List<T>
        }
    }
}