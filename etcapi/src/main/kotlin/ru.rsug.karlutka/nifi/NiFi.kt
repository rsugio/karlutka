package ru.rsug.karlutka.nifi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.util.function.Predicate

class NiFi {
    enum class EComponentType { PROCESS_GROUP, CONNECTION }

    @Serializable
    class NFlowDefinition(
        val externalControllerServices: Map<String, JsonElement>,
        val flowContents: NFlowContents,
        val flowEncodingVersion: String,
        val parameterContexts: Map<String, JsonElement>,
    )

    @Serializable
    class NFlowContents(
        val comments: String,
        val componentType: EComponentType,
        val connections: List<NConnection>,
        val controllerServices: List<JsonObject>,
        val defaultBackPressureDataSizeThreshold: String,
        val defaultBackPressureObjectThreshold: Int,
        val defaultFlowFileExpiration: String,
        val flowFileConcurrency: String,
        val flowFileOutboundPolicy: String,
        val funnels: List<JsonObject>,
        val groupIdentifier: String? = null,
        val identifier: String, //guid
        val inputPorts: List<JsonObject>,
        val instanceIdentifier: String, //guid
        val labels: List<Map<String, JsonElement>>,
        val name: String,
        val outputPorts: List<JsonObject>,
        val position: Map<String, Float>,
        val processGroups: List<NFlowContents>,
        val processors: List<NProcessor>,
        val remoteProcessGroups: List<JsonObject>,
        val variables: Map<String, String>,
    )

    @Serializable
    class NConnection(
        val backPressureDataSizeThreshold: String,
        val backPressureObjectThreshold: Int,
        val bends: List<Map<String, Float>>,
        val componentType: String,
        val destination: Map<String, String>,
        val flowFileExpiration: String,
        val groupIdentifier: String,
        val identifier: String,
        val instanceIdentifier: String,
        val labelIndex: Int,
        val loadBalanceCompression: String,
        val loadBalanceStrategy: String,
        val name: String,
        val partitioningAttribute: String,
        val prioritizers: List<String>,
        val selectedRelationships: List<String>,
        val source: Map<String, String>,
        val zIndex: Int,
    )

    @Serializable
    class NProcessor(
        val annotationData: String? = null,
        val autoTerminatedRelationships: List<String>,
        val backoffMechanism: String,
        val bulletinLevel: String,
        val bundle: Map<String, String>,
        val comments: String,
        val componentType: String,
        val concurrentlySchedulableTaskCount: Int,
        val executionNode: String,
        val groupIdentifier: String,
        val identifier: String,
        val instanceIdentifier: String,
        val maxBackoffPeriod: String,
        val name: String,
        val penaltyDuration: String,
        val position: Map<String, Float>,
        val properties: Map<String, String?>,
        val propertyDescriptors: Map<String, Map<String, JsonElement>>,
        val retriedRelationships: List<String>,
        val retryCount: Int,
        val runDurationMillis: Int,
        val scheduledState: String,
        val schedulingPeriod: String,
        val schedulingStrategy: String,
        val style: Map<String, String>,
        val type: String,
        val yieldDuration: String,
    )

    data class TL(val name: String, val variables: Map<String, String> = mapOf()) {
        val children = mutableListOf<TL>()
        fun collectvars(parent: MutableMap<String, String>, isok: Predicate<Map.Entry<String, String>>) {
            variables.forEach { v ->
                if (isok.test(v)) {
                    parent[v.key] = v.value
                }
            }
            children.forEach { x ->
                x.collectvars(parent, isok)
            }
        }
    }


    companion object {
        private fun rec(depth: Int, c: NFlowContents, parent: TL? = null): TL {
            val tl = TL(c.name, c.variables)
            parent?.children?.add(tl)
            c.processGroups.forEach { t ->
                rec(depth + 1, t, tl)
            }
            return tl
        }

        private fun rec(d: NFlowDefinition): TL {
            val root = TL("/")
            d.flowContents.processGroups.forEach { t ->
                rec(0, t, root)
            }
            return root
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun parseStream(`is`: InputStream): TL {
            val d = Json.decodeFromStream<NFlowDefinition>(`is`)
            return rec(d)
        }

        fun parseString(s: String): TL {
            val d = Json.decodeFromString<NFlowDefinition>(s)
            return rec(d)
        }
    }
}