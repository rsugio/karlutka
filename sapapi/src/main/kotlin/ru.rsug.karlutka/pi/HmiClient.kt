package ru.rsug.karlutka.pi

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedGenerator
import java.util.*

class HmiClient(
    private val typeId: String,
    private val typeIdInput: String,
    private val serviceId: String,
    private val methodId: String,
    private val clientLevel: HmiUsages.ApplCompLevel,
    private val clientUser: String,
    private val clientPassword: String? = null,
    private val clientLanguage: String = "EN",
) {
    private val uuidgenerator: TimeBasedGenerator = Generators.timeBasedGenerator()

    private val clientId: UUID = uuidgenerator.generate()!!
    private var requestId: UUID = uuidgenerator.generate()

    private fun UUID.toShortStringLower(): String {
        return this.toString().replace("-", "", false)
    }

    fun request(params: Map<String, String>): Hmi.HmiRequest {
        val req = Hmi.HmiRequest(
            typeId, clientId.toShortStringLower(), requestId.toShortStringLower(), clientLevel,
            typeIdInput,
            params,
            methodId, serviceId, clientUser, clientPassword, clientLanguage
        )
        requestId = uuidgenerator.generate()
        return req
    }
    fun request(name: String, value: String) = request(mapOf(name to value))

    fun parseResponse(instance: Hmi.Instance) : Hmi.HmiResponse {
        // преобразуем его в HmiResponse
        val response = Hmi.HmiResponse(instance)
        return response
    }

    companion object {
        fun simpleQueryClient(version: String = "7.5", user: String) = HmiClient(
            Hmi.typeIdAiiHmiRequest, Hmi.typeIdAiiHmiInput, "QUERY", "GENERIC", HmiUsages.ApplCompLevel(version), user)

    }
}