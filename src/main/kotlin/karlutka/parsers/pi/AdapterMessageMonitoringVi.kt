package karlutka.parsers.pi

import karlutka.models.PIModel
import karlutka.serialization.KSoap.*
import karlutka.serialization.KSoap.Companion.xmlserializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.util.*

class AdapterMessageMonitoringVi {

    // ---------------
    @Serializable
    @XmlSerialName("cancelMessages", "urn:AdapterMessageMonitoringVi", "amvi")
    class CancelMessages(
        @XmlElement(true)
        @XmlSerialName("messageKeys", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKeys: ArrayOfStrings,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("cancelMessagesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class CancelMessagesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Resp: AdminActionResultMap,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): AdminActionResultMap? {
                val x = xmlserializer.decodeFromString<Envelope<CancelMessagesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("failEoioMessage", "urn:AdapterMessageMonitoringVi", "amvi")
    data class FailEoioMessage(
        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("retriggerSuccessor", "urn:AdapterMessageMonitoringVi", "amvi")
        val retriggerSuccessor: Boolean,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("failEoioMessageResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class FailEoioMessageResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: AdminActionResult,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): AdminActionResult? {
                val x = xmlserializer.decodeFromString<Envelope<FailEoioMessageResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getAllAvailableStatusDetails", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetAllAvailableStatusDetails(
        @XmlElement(true)
        @XmlSerialName("locale", "urn:AdapterMessageMonitoringVi", "amvi")
        val locale: LocaleAMM,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getAllAvailableStatusDetailsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetAllAvailableStatusDetailsResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfStatusDetail,
    ) : ComposeSOAP() {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<StatusDetail> {
                val x = xmlserializer.decodeFromString<Envelope<GetAllAvailableStatusDetailsResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.list ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getConnections", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetConnections : ComposeSOAP()

    @Serializable
    @XmlSerialName("getConnectionsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetConnectionsResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "urn:AdapterMessageMonitoringVi", "amvi")
        val Response: ArrayOfStrings,
    ) : ComposeSOAP() {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<String> {
                val x = xmlserializer.decodeFromString<Envelope<GetConnectionsResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.strings ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getErrorCodes", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetErrorCodes(
        @XmlElement(true)
        @XmlSerialName("errorLabelID", "urn:AdapterMessageMonitoringVi", "amvi")
        val errorLabelID: Int,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getErrorCodesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetErrorCodesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfStrings,
    ) : ComposeSOAP() {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<String> {
                val x = xmlserializer.decodeFromString<Envelope<GetErrorCodesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.strings ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getIntegrationFlows", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetIntegrationFlows(
        @XmlElement(true)
        @XmlSerialName("language", "urn:AdapterMessageMonitoringVi", "amvi")
        val language: String,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getIntegrationFlowsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetIntegrationFlowResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfIntegrationFlow,
    ) : ComposeSOAP() {
        @Serializable
        data class ArrayOfIntegrationFlow(
            val list: MutableList<IntegrationFlow> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("IntegrationFlow", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        data class IntegrationFlow(
            @XmlElement(true)
            @XmlSerialName("name", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
            val name: String? = null,
            @XmlElement(true)
            @XmlSerialName("description", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
            val description: String? = null,
            @XmlElement(true)
            @XmlSerialName("id", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
            val id: String? = null,
        )

        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<IntegrationFlow> {
                val x = xmlserializer.decodeFromString<Envelope<GetIntegrationFlowResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.list ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getInterfaces", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetInterfaces : ComposeSOAP()

    @Serializable
    @XmlSerialName("getInterfacesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetInterfacesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,   //TODO он пустой и нужны нормальные тестовые данные
    ) : ComposeSOAP() {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): String? {
                val x = xmlserializer.decodeFromString<Envelope<GetInterfacesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response //TODO изменить
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getLogEntries", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetLogEntries(
        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean,
        @XmlElement(true)
        @XmlSerialName("maxResults", "urn:AdapterMessageMonitoringVi", "amvi")
        val maxResults: Int? = null,
        //TODO locale
        @XmlElement(true)
        @XmlSerialName("olderThan", "urn:AdapterMessageMonitoringVi", "amvi")
        val olderThan: String, //xs:dateTime
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getLogEntriesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetLogEntriesResponse(
        // бага в сервисе - может вернутся Response в одном из двух неймспейсов
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        private val Response: ArrayOfAuditLogEntryData?,
        @XmlElement(true)
        @XmlSerialName("Response", "urn:AdapterMessageMonitoringVi", "amvi")
        private val Response2: ArrayOfAuditLogEntryData?,
    ) {
        @Serializable
        data class ArrayOfAuditLogEntryData(
            val AuditLogEntryData: MutableList<AuditLogEntryData>?,
        )

        @Serializable
        @XmlSerialName("AuditLogEntryData", "urn:com.sap.aii.mdt.api.data", "mdt")
        data class AuditLogEntryData(
            @XmlElement(true)
            @XmlSerialName("timeStamp", "urn:com.sap.aii.mdt.api.data", "mdt")
            val timeStamp: String?, //xs:dateTime
            @XmlElement(true)
            @XmlSerialName("textKey", "urn:com.sap.aii.mdt.api.data", "mdt")
            val textKey: String?,
            @XmlElement(true)
            @XmlSerialName("params", "urn:com.sap.aii.mdt.api.data", "mdt")
            val params: ArrayOfStrings,
            @XmlElement(true)
            @XmlSerialName("status", "urn:com.sap.aii.mdt.api.data", "mdt")
            val status: String?,
            @XmlElement(true)
            @XmlSerialName("localizedText", "urn:com.sap.aii.mdt.api.data", "mdt")
            val localizedText: String?,
        )

        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<AuditLogEntryData> {
                val x = xmlserializer.decodeFromString<Envelope<GetLogEntriesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.AuditLogEntryData ?: x.body.data?.Response2?.AuditLogEntryData
                ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getLoggedMessageBytes", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetLoggedMessageBytes(
        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("version", "urn:AdapterMessageMonitoringVi", "amvi")
        val version: String,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getLoggedMessageBytesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetLoggedMessageBytesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        private val Response: String = "", //xs:base64Binary
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): ByteArray {
                val x = xmlserializer.decodeFromString<Envelope<GetLoggedMessageBytesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return Base64.getDecoder().decode(x.body.data?.Response ?: "")
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringBoolean", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetMessageBytesJavaLangStringBoolean(
        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringBooleanResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetMessageBytesJavaLangStringBooleanResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        private val Response: String = "", //xs:base64Binary
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): ByteArray {
                val x = xmlserializer.decodeFromString<Envelope<GetMessageBytesJavaLangStringBooleanResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return Base64.getDecoder().decode(x.body.data?.Response ?: "")
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringIntBoolean", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetMessageBytesJavaLangStringIntBoolean(
        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("version", "urn:AdapterMessageMonitoringVi", "amvi")
        val version: Int,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringIntBooleanResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetMessageBytesJavaLangStringIntBooleanResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        private val Response: String = "", //xs:base64Binary
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): ByteArray {
                val x = xmlserializer.decodeFromString<Envelope<GetMessageBytesJavaLangStringIntBooleanResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return Base64.getDecoder().decode(x.body.data?.Response ?: "")
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getMessageList", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetMessageList(
        @XmlElement(true)
        @XmlSerialName("filter", "urn:AdapterMessageMonitoringVi", "amvi")
        val filter: AdapterFilter,
        @XmlElement(true)
        @XmlSerialName("maxMessages", "urn:AdapterMessageMonitoringVi", "amvi")
        val maxMessages: Int? = null,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessageListResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetMessageListResponse(
        // имя не в XML
        val Resp: Response,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): Response? {
                val x = xmlserializer.decodeFromString<Envelope<GetMessageListResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getMessagesByIDs", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetMessagesByIDs(
        @XmlElement(true)
        @XmlSerialName("messageIds", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageIds: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("referenceIds", "urn:AdapterMessageMonitoringVi", "amvi")
        val referenceIds: ArrayOfStrings = ArrayOfStrings(),
        @XmlElement(true)
        @XmlSerialName("correlationIds", "urn:AdapterMessageMonitoringVi", "amvi")
        val correlationIds: ArrayOfStrings = ArrayOfStrings(),
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessagesByIDsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetMessagesByIDsResponse(
        val Resp: Response,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): Response? {
                val x = xmlserializer.decodeFromString<Envelope<GetMessagesByIDsResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getMessagesByKeys", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetMessagesByKeys(
        @XmlElement(true)
        @XmlSerialName("filter", "urn:AdapterMessageMonitoringVi", "amvi")
        val filter: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("maxMessages", "urn:AdapterMessageMonitoringVi", "amvi")
        val maxMessages: Int? = null,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessagesByKeysResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetMessagesByKeysResponse(
        val Resp: Response,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): Response? {
                val x = xmlserializer.decodeFromString<Envelope<GetMessagesByKeysResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }
// ---------------

    @Serializable
    @XmlSerialName("getMessagesWithSuccessors", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetMessagesWithSuccessors(
        @XmlElement(true)
        @XmlSerialName("messageIds", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageIds: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessagesWithSuccessorsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetMessagesWithSuccessorsResponse(
        val Resp: Response,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): Response? {
                val x = xmlserializer.decodeFromString<Envelope<GetMessagesWithSuccessorsResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getParties", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetParties : ComposeSOAP()

    @Serializable
    @XmlSerialName("getPartiesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetPartiesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): String? {
                val x = xmlserializer.decodeFromString<Envelope<GetPredecessorMessageIdResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return null //TODO
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getPredecessorMessageId", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetPredecessorMessageId(
        @XmlElement(true)
        @XmlSerialName("messageId", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageIds: String,
        @XmlElement(true)
        @XmlSerialName("direction", "urn:AdapterMessageMonitoringVi", "amvi")
        val direction: String,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getPredecessorMessageIdResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetPredecessorMessageIdResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    ) : ComposeSOAP() {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): String? {
                val x = xmlserializer.decodeFromString<Envelope<GetPredecessorMessageIdResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getServices", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetServices : ComposeSOAP()

    @Serializable
    @XmlSerialName("getServicesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetServicesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): String? {
                val x = xmlserializer.decodeFromString<Envelope<GetPredecessorMessageIdResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return null //TODO
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getStatusDetails", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetStatusDetails(
        @XmlElement(true)
        @XmlSerialName("errorCodes", "urn:AdapterMessageMonitoringVi", "amvi")
        val errorCodes: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("locale", "urn:AdapterMessageMonitoringVi", "amvi")
        val locale: LocaleAMM,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getStatusDetailsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetStatusDetailsResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfStatusDetail,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<StatusDetail> {
                val x = xmlserializer.decodeFromString<Envelope<GetStatusDetailsResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.list ?: mutableListOf()
            }
        }
    }
// ---------------

    @Serializable
    @XmlSerialName("getUserDefinedSearchAttributes", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetUserDefinedSearchAttributes(
        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:AdapterMessageMonitoringVi", "amvi")
        val archive: Boolean,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchAttributesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetUserDefinedSearchAttributesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Resp: ArrayOfBusinessAttribute,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<BusinessAttribute> {
                val x = xmlserializer.decodeFromString<Envelope<GetUserDefinedSearchAttributesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp?.list ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getUserDefinedSearchExtractors", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetUserDefinedSearchExtractors(
        @XmlElement(true)
        @XmlSerialName("name", "urn:AdapterMessageMonitoringVi", "amvi")
        val name: String,
        @XmlElement(true)
        @XmlSerialName("namespace", "urn:AdapterMessageMonitoringVi", "amvi")
        val namespace: String,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchExtractorsResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetUserDefinedSearchExtractorsResponse(
        @XmlElement(true)
        private val Response: _RplResponse,
    ) {
        @Serializable
        @XmlSerialName("Response", "urn:AdapterMessageMonitoringVi", "amvi")
        data class _RplResponse(
            val AttributeMetadata: MutableList<AttributeMetadata>,
        )

        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<AttributeMetadata> {
                val x = xmlserializer.decodeFromString<Envelope<GetUserDefinedSearchExtractorsResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.AttributeMetadata ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getUserDefinedSearchFilters", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetUserDefinedSearchFilters : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchFiltersResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    data class GetUserDefinedSearchFiltersResponse(
        @XmlElement(true)
        private val Response: _RplResponse,
    ) {
        @Serializable
        @XmlSerialName("Response", "urn:AdapterMessageMonitoringVi", "amvi")
        data class _RplResponse(
            val MessageInterface: MutableList<MessageInterface>,
        )

        companion object {
            fun parseSOAP(sxml: String, f: Fault): MutableList<MessageInterface> {
                val x = xmlserializer.decodeFromString<Envelope<GetUserDefinedSearchFiltersResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Response?.MessageInterface ?: mutableListOf()
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("getUserDefinedSearchMessages", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetUserDefinedSearchMessages(
        @XmlElement(true)
        @XmlSerialName("filter", "urn:AdapterMessageMonitoringVi", "amvi")
        val filter: AdapterFilter,
        @XmlElement(true)
        @XmlSerialName("maxMessages", "urn:AdapterMessageMonitoringVi", "amvi")
        val maxMessages: Int? = null,
        @XmlElement(true)
        @XmlSerialName("attributes", "urn:AdapterMessageMonitoringVi", "amvi")
        val attributes: ArrayOfBusinessAttribute,
        @XmlElement(true)
        @XmlSerialName("operator", "urn:AdapterMessageMonitoringVi", "amvi")
        val operator: String,   //AND, OR
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchMessagesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class GetUserDefinedSearchMessagesResponse(
        // имя не в XML
        val Resp: Response,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): Response? {
                val x = xmlserializer.decodeFromString<Envelope<GetUserDefinedSearchMessagesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }

    // ---------------
    @Serializable
    @XmlSerialName("resendMessages", "urn:AdapterMessageMonitoringVi", "amvi")
    class ResendMessages(
        @XmlElement(true)
        @XmlSerialName("messageKeys", "urn:AdapterMessageMonitoringVi", "amvi")
        val messageKeys: ArrayOfStrings,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("resendMessagesResponse", "urn:AdapterMessageMonitoringVi", "amvi")
    class ResendMessagesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Resp: AdminActionResultMap,
    ) {
        companion object {
            fun parseSOAP(sxml: String, f: Fault): AdminActionResultMap? {
                val x = xmlserializer.decodeFromString<Envelope<ResendMessagesResponse>>(sxml)
                f.faultcode = x.body.fault?.faultcode ?: ""
                f.faultstring = x.body.fault?.faultstring ?: ""
                return x.body.data?.Resp
            }
        }
    }

    // ---------------
    @Serializable
    data class LocaleAMM(
        @XmlElement(true)
        @XmlSerialName("language", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val language: String? = null,
        @XmlElement(true)
        @XmlSerialName("country", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val country: String? = null,
        @XmlElement(true)
        @XmlSerialName("variant", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val variant: String? = null,
    )

    @Serializable
    data class ArrayOfStatusDetail(
        @XmlSerialName("StatusDetail", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val list: MutableList<StatusDetail> = mutableListOf(),
    )

    @Serializable
    data class StatusDetail(
        @XmlElement(true)
        @XmlSerialName("errorLabelID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val errorLabelID: String? = null,
        @XmlElement(true)
        @XmlSerialName("description", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val description: String? = null,
        @XmlElement(true)
        @XmlSerialName("text", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val text: String? = null,
    )

    @Serializable
    class AdapterFilter(
        // см.file2.wsdl AdapterFilter
        // все поля сделаны изменяемыми для удобства кода
        @XmlElement(true)
        @XmlSerialName("applicationComponent", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var applicationComponent: String? = null,
        @XmlElement(true)
        @XmlSerialName("archive", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var archive: Boolean = false,

        @XmlElement(true)
        @XmlSerialName("connectionName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var connectionName: String? = null,

        @XmlElement(true)
        @XmlSerialName("correlationID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var correlationID: String? = null,

        @XmlElement(true)
        @XmlSerialName("dateType", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var dateType: Int = 0,

        @XmlElement(true)
        @XmlSerialName("direction", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var direction: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCategory", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var errorCategory: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCode", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var errorCode: String? = null,

        @XmlElement(true)
        @XmlSerialName("fromTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var fromTime: String? = null,   //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("interface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var interface_: rn2Interface? = null,

        @XmlElement(true)
        @XmlSerialName("messageFormat", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var messageFormat: String? = null,

        @XmlElement(true)
        @XmlSerialName("messageIDs", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var messageIDs: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("messageType", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var messageType: String? = null,

        @XmlElement(true)
        @XmlSerialName("nodeId", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var nodeId: Long = 0,

        @XmlElement(true)
        @XmlSerialName("onlyFaultyMessages", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var onlyFaultyMessages: Boolean = false,

        @XmlElement(true)
        @XmlSerialName("persistUntil", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var persistUntil: String? = null,   //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("protocol", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var protocol: String? = null,

        @XmlElement(true)
        @XmlSerialName("qualityOfService", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var qualityOfService: String? = null,

        @XmlElement(true)
        @XmlSerialName("receiverInterface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var receiverInterface: rn2Interface? = null,

        @XmlElement(true)
        @XmlSerialName("receiverName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var receiverName: String? = null,

        @XmlSerialName("receiverParty", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var receiverParty: rn2Party? = null,

        @XmlElement(true)
        @XmlSerialName("referenceIDs", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var referenceIDs: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("retries", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var retries: Int = 0,

        @XmlElement(true)
        @XmlSerialName("retryInterval", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var retryInterval: Long = 0,

        @XmlElement(true)
        @XmlSerialName("scheduleTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var scheduleTime: String? = null,   //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("senderInterface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var senderInterface: rn2Interface? = null,

        @XmlElement(true)
        @XmlSerialName("senderName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var senderName: String? = null,

        @XmlSerialName("senderParty", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var senderParty: rn2Party? = null,

        @XmlElement(true)
        @XmlSerialName("sequenceID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var sequenceID: String? = null,

        @XmlElement(true)
        @XmlSerialName("serializationContext", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var serializationContext: String? = null,

        @XmlElement(true)
        @XmlSerialName("serviceDefinition", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var serviceDefinition: String? = null,

        @XmlElement(true)
        @XmlSerialName("softwareComponent", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var softwareComponent: String? = null,

        @XmlElement(true)
        @XmlSerialName("status", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var status: String? = null,

        @XmlElement(true)
        @XmlSerialName("statuses", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var statuses: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("timesFailed", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var timesFailed: Int = 0,

        @XmlElement(true)
        @XmlSerialName("toTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var toTime: String? = null, //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("transport", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var transport: String? = null,

        @XmlElement(true)
        @XmlSerialName("validUntil", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var validUntil: String? = null, //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("wasEdited", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var wasEdited: Boolean = false,

        @XmlElement(true)
        @XmlSerialName("scenarioIDs", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var scenarioIDs: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("rootID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var rootID: String? = null,

        @XmlElement(true)
        @XmlSerialName("returnLogLocations", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var returnLogLocations: Boolean? = null,

        @XmlElement(true)
        @XmlSerialName("onlyLogLocationsWithPayload", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        var onlyLogLocationsWithPayload: Boolean? = null,
    )

    @Serializable
    @XmlSerialName("Response", "", "")
    class Response(
        @XmlElement(true)
        @XmlSerialName("date", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val date: String = "",  //в 7.4 его нет
        @XmlElement(true)
        val afw: LAFW? = null,
        @XmlElement(true)
        @XmlSerialName("number", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val number: Int,
        @XmlElement(true)
        @XmlSerialName("warning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val warning: Boolean,
        @XmlElement(true)
        @XmlSerialName("displayPermissionWarning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val displayPermissionWarning: Boolean,
    )

    @Serializable
    @XmlSerialName("list", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
    class LAFW(val list: List<AdapterFrameworkData>)

    @Serializable
    @XmlSerialName("AdapterFrameworkData", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
    class AdapterFrameworkData(
        // см. file2.wsdl для complexType name="AdapterFrameworkData", почти все поля необязательные
        @XmlElement(true)
        @XmlSerialName("applicationComponent", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val applicationComponent: String? = null,

        @XmlElement(true)
        @XmlSerialName("businessMessage", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val businessMessage: rn2Boolean? = null,

        @XmlElement(true)
        @XmlSerialName("cancelable", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val cancelable: rn2Boolean? = null, // = rn2Boolean(false),

        @XmlElement(true)
        @XmlSerialName("connectionName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val connectionName: String? = null,

        @XmlElement(true)
        @XmlSerialName("credential", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val credential: String? = null,

        @XmlElement(true)
        @XmlSerialName("direction", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val direction: PIModel.DIRECTION? = null, // EnumDirection.OUTBOUND,

        @XmlSerialName("editable", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val editable: rn2Boolean? = null, // = rn2Boolean(false),

        @XmlElement(true)
        @XmlSerialName("endTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val endTime: String? = null,    //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("endpoint", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val endpoint: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCategory", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val errorCategory: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCode", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val errorCode: String? = null,

        @XmlElement(true)
        @XmlSerialName("headers", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val headers: String? = null,

        @XmlSerialName("interface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val interface_: rn2Interface? = null, //rn2Interface("a", "urn:a"),

        @XmlElement(true)
        @XmlSerialName("isPersistent", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val isPersistent: Boolean,

        @XmlElement(true)
        @XmlSerialName("messageID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val messageID: String?,

        @XmlElement(true)
        @XmlSerialName("messageKey", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val messageKey: String?,

        @XmlElement(true)
        @XmlSerialName("messageType", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val messageType: String?,

        @XmlElement(true)
        @XmlSerialName("nodeId", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val nodeId: Int,

        @XmlElement(true)
        @XmlSerialName("persistUntil", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val persistUntil: String?,  //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("protocol", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val protocol: String?,

        @XmlElement(true)
        @XmlSerialName("qualityOfService", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val qualityOfService: String?,

        @XmlSerialName("receiverInterface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val receiverInterface: rn2Interface?, // = rn2Interface("SI_Sync", "urn:"),

        @XmlElement(true)
        @XmlSerialName("receiverName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val receiverName: String?,

        @XmlSerialName("receiverParty", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val receiverParty: rn2Party?, // = rn2Party("http://sap.com/xi/XI", "", "XIParty"),

        @XmlElement(true)
        @XmlSerialName("referenceID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val referenceID: String?,

        @XmlSerialName("restartable", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val restartable: rn2Boolean?, // = rn2Boolean(false),

        @XmlElement(true)
        @XmlSerialName("retries", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val retries: Int,

        @XmlElement(true)
        @XmlSerialName("retryInterval", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val retryInterval: Long = 0,

        @XmlElement(true)
        @XmlSerialName("scheduleTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val scheduleTime: String?,  //xs:dateTime

        @XmlSerialName("senderInterface", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val senderInterface: rn2Interface?, // = rn2Interface("SI_Sync", "urn:"),

        @XmlElement(true)
        @XmlSerialName("senderName", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val senderName: String?,

        @XmlSerialName("senderParty", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val senderParty: rn2Party?, // = rn2Party("http://sap.com/xi/XI", "", "XIParty"),

        @XmlElement(true)
        @XmlSerialName("sequenceNumber", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val sequenceNumber: Long?,

        @XmlElement(true)
        @XmlSerialName("serializationContext", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val serializationContext: String?,

        @XmlElement(true)
        @XmlSerialName("serviceDefinition", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val serviceDefinition: String?,

        @XmlElement(true)
        @XmlSerialName("softwareComponent", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val softwareComponent: String?,

        @XmlElement(true)
        @XmlSerialName("startTime", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val startTime: String?, //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("status", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val status: String?,

        @XmlElement(true)
        @XmlSerialName("timesFailed", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val timesFailed: Long,

        @XmlElement(true)
        @XmlSerialName("transport", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val transport: String?,

        @XmlElement(true)
        @XmlSerialName("validUntil", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val validUntil: String?,    //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("version", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val version: String?,

        @XmlSerialName("wasEdited", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val wasEdited: rn2Boolean?, // = rn2Boolean(false),

        @XmlSerialName("businessAttributes", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val businessAttributes: ArrayOfBusinessAttribute?,

        @XmlElement(true)
        @XmlSerialName("payloadPermissionWarning", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val payloadPermissionWarning: rn2Boolean?,

        @XmlElement(true)
        @XmlSerialName("errorLabel", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val errorLabel: Int,

        @XmlElement(true)
        @XmlSerialName("scenarioIdentifier", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val scenarioIdentifier: String?,

        @XmlElement(true)
        @XmlSerialName("parentID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val parentID: String?,

        @XmlSerialName("duration", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val duration: rn2Duration?, // = rn2Duration(0),

        @XmlElement(true)
        @XmlSerialName("size", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val size: Long?,

        @XmlElement(true)
        @XmlSerialName("messagePriority", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val messagePriority: Long?,

        @XmlElement(true)
        @XmlSerialName("rootID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val rootID: String?,

        @XmlElement(true)
        @XmlSerialName("sequenceID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val sequenceID: String?, //CPQQUOTE_QUEUE

        @XmlElement(true)
        @XmlSerialName("logLocations", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val logLocations: ArrayOfStrings?,

        @XmlElement(true)
        @XmlSerialName("passport", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val passport: String?,//2A54482A0300E60000000000000000000000000000000000000002A54482A

        @XmlElement(true)
        @XmlSerialName("passportTID", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val passportTID: String?,//005056bf-8d88-1eed-84ae-893f641f6670
    )

    @Serializable
    data class AdminActionResultMap(
        @XmlElement(true)
        @XmlSerialName("keyList", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val keyList: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("resultList", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val resultList: AdminActionResultList,
    )

    @Serializable
    data class AdminActionResultList(
        @XmlElement(true)
        @XmlSerialName("AdminActionResult", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val list: MutableList<AdminActionResult>,
    )

    @Serializable
    data class AdminActionResult(
        @XmlElement(true)
        @XmlSerialName("resultCode", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val resultCode: String?,
        @XmlElement(true)
        @XmlSerialName("resultText", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val resultText: String?,
        @XmlElement(true)
        @XmlSerialName("successful", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val successful: Boolean,
    )

    @Serializable
    data class ArrayOfBusinessAttribute(
        @XmlElement(true)
        val list: MutableList<BusinessAttribute>?,
    )

    @Serializable
    @XmlSerialName("BusinessAttribute", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
    data class BusinessAttribute(
        @XmlElement(true)
        @XmlSerialName("name", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val name: String,
        @XmlElement(true)
        @XmlSerialName("value", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val value: String,
    )

    @Serializable
    @XmlSerialName("MessageInterface", "urn:com.sap.aii.mdt.api.data", "rn2")
    data class MessageInterface(
        @XmlElement(true)
        @XmlSerialName("name", "urn:com.sap.aii.mdt.api.data", "rn2")
        val name: String,
        @XmlElement(true)
        @XmlSerialName("namespace", "urn:com.sap.aii.mdt.api.data", "rn2")
        val namespace: String,
        @XmlElement(true)
        @XmlSerialName("senderParty", "urn:com.sap.aii.mdt.api.data", "rn2")
        val senderParty: String,
        @XmlElement(true)
        @XmlSerialName("senderComponent", "urn:com.sap.aii.mdt.api.data", "rn2")
        val senderComponent: String,
        @XmlElement(true)
        @XmlSerialName("receiverParty", "urn:com.sap.aii.mdt.api.data", "rn2")
        val receiverParty: String,
        @XmlElement(true)
        @XmlSerialName("receiverComponent", "urn:com.sap.aii.mdt.api.data", "rn2")
        val receiverComponent: String,
    )

    @Serializable
    @XmlSerialName("AttributeMetadata", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
    data class AttributeMetadata(
        @XmlElement(true)
        @XmlSerialName("name", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val name: String,
        @XmlElement(true)
        @XmlSerialName("description", "urn:com.sap.aii.mdt.server.adapterframework.ws", "afw")
        val description: String,
    )

    @Serializable
    class rn2Boolean(
        @XmlElement(true)
        @XmlSerialName("value", "urn:com.sap.aii.mdt.api.data", "rn2")
        val value: Boolean?,
    )

    @Serializable
    class rn2Duration(
        @XmlElement(true)
        @XmlSerialName("duration", "urn:com.sap.aii.mdt.api.data", "rn2")
        val duration: Int,
    )

    @Serializable
    class rn2Interface(
        @XmlElement(true)
        @XmlSerialName("name", "urn:com.sap.aii.mdt.api.data", "rn2")
        val name: String,
        @XmlElement(true)
        @XmlSerialName("namespace", "urn:com.sap.aii.mdt.api.data", "rn2")
        val namespace: String,
    )

    @Serializable
    class ArrayOfStrings(
        @XmlElement(true)
        @XmlSerialName("String", "urn:java/lang", "rn6")
        val strings: MutableList<String> = mutableListOf(),
    ) {
        constructor(vararg s: String) : this(s.toMutableList())
    }

    @Serializable
    class rn2Party(
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

}