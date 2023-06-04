package ru.rsug.karlutka.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import ru.rsug.karlutka.serialization.KSoap.ComposeSOAP

private const val xmlnsAMVi = "urn:AdapterMessageMonitoringVi"
private const val amvi = "amvi"
private const val xmlnsAfw = "urn:com.sap.aii.mdt.server.adapterframework.ws"
private const val afwp = "afw"
private const val xmlnsPPVi = "urn:ProfileProcessorVi"
private const val ppvi = "ppvi"
private const val xmlnsStat = "urn:com.sap.aii.af.service.statistic.ws.impl"
private const val stat = "stat"

class AdapterMessageMonitoringVi {
    // ---------------
    @Serializable
    @XmlSerialName("cancelMessages", xmlnsAMVi, amvi)
    class CancelMessages(
        @XmlElement(true)
        @XmlSerialName("messageKeys", xmlnsAMVi, amvi)
        val messageKeys: ArrayOfStrings,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("cancelMessagesResponse", xmlnsAMVi, amvi)
    class CancelMessagesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Resp: AdminActionResultMap,
    )

    // ---------------
    @Serializable
    @XmlSerialName("failEoioMessage", xmlnsAMVi, amvi)
    class FailEoioMessage(
        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAMVi, amvi)
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("retriggerSuccessor", xmlnsAMVi, amvi)
        val retriggerSuccessor: Boolean,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("failEoioMessageResponse", xmlnsAMVi, amvi)
    class FailEoioMessageResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: AdminActionResult,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getAllAvailableStatusDetails", xmlnsAMVi, amvi)
    class GetAllAvailableStatusDetails(
        @XmlElement(true)
        @XmlSerialName("locale", xmlnsAMVi, amvi)
        val locale: LocaleAMM,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getAllAvailableStatusDetailsResponse", xmlnsAMVi, amvi)
    class GetAllAvailableStatusDetailsResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfStatusDetail,
    ) : ComposeSOAP()

    // ---------------
    @Serializable
    @XmlSerialName("getConnections", xmlnsAMVi, amvi)
    class GetConnections : ComposeSOAP()

    @Serializable
    @XmlSerialName("getConnectionsResponse", xmlnsAMVi, amvi)
    class GetConnectionsResponse(
        @XmlElement(true)
        @XmlSerialName("Response", xmlnsAMVi, amvi)
        val Response: ArrayOfStrings,
    ) : ComposeSOAP()

    // ---------------
    @Serializable
    @XmlSerialName("getErrorCodes", xmlnsAMVi, amvi)
    class GetErrorCodes(
        @XmlElement(true)
        @XmlSerialName("errorLabelID", xmlnsAMVi, amvi)
        val errorLabelID: Int,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getErrorCodesResponse", xmlnsAMVi, amvi)
    class GetErrorCodesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfStrings,
    ) : ComposeSOAP()

    // ---------------
    @Serializable
    @XmlSerialName("getIntegrationFlows", xmlnsAMVi, amvi)
    class GetIntegrationFlows(
        @XmlElement(true)
        @XmlSerialName("language", xmlnsAMVi, amvi)
        val language: String,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getIntegrationFlowsResponse", xmlnsAMVi, amvi)
    class GetIntegrationFlowResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfIntegrationFlow,
    ) : ComposeSOAP() {
        @Serializable
        class ArrayOfIntegrationFlow(
            val list: List<IntegrationFlow> = listOf(),
        )

        @Serializable
        @XmlSerialName("IntegrationFlow", xmlnsAfw, afwp)
        class IntegrationFlow(
            @XmlElement(true)
            @XmlSerialName("name", xmlnsAfw, afwp)
            val name: String? = null,
            @XmlElement(true)
            @XmlSerialName("description", xmlnsAfw, afwp)
            val description: String? = null,
            @XmlElement(true)
            @XmlSerialName("id", xmlnsAfw, afwp)
            val id: String? = null,
        )
    }

    // ---------------
    @Serializable
    @XmlSerialName("getInterfaces", xmlnsAMVi, amvi)
    class GetInterfaces : ComposeSOAP()

    @Serializable
    @XmlSerialName("getInterfacesResponse", xmlnsAMVi, amvi)
    class GetInterfacesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    ) : ComposeSOAP()

    // ---------------
    @Serializable
    @XmlSerialName("getLogEntries", xmlnsAMVi, amvi)
    class GetLogEntries(
        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAMVi, amvi)
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean,
        @XmlElement(true)
        @XmlSerialName("maxResults", xmlnsAMVi, amvi)
        val maxResults: Int? = null,
        @XmlElement(true)
        @XmlSerialName("olderThan", xmlnsAMVi, amvi)
        val olderThan: String, //xs:dateTime
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getLogEntriesResponse", xmlnsAMVi, amvi)
    class GetLogEntriesResponse(
        // бага в сервисе - может вернутся Response в одном из двух неймспейсов
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfAuditLogEntryData?,
        @XmlElement(true)
        @XmlSerialName("Response", xmlnsAMVi, amvi)
        val Response2: ArrayOfAuditLogEntryData?,
    ) {
        fun get() = Response?.AuditLogEntryData ?: Response2?.AuditLogEntryData ?: listOf()

        @Serializable
        class ArrayOfAuditLogEntryData(
            val AuditLogEntryData: List<AuditLogEntryData> = listOf(),
        )

        @Serializable
        @XmlSerialName("AuditLogEntryData", "urn:com.sap.aii.mdt.api.data", "mdt")
        class AuditLogEntryData(
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
    }

    // ---------------
    @Serializable
    @XmlSerialName("getLoggedMessageBytes", xmlnsAMVi, amvi)
    class GetLoggedMessageBytes(
        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAMVi, amvi)
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("version", xmlnsAMVi, amvi)
        val version: String,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getLoggedMessageBytesResponse", xmlnsAMVi, amvi)
    class GetLoggedMessageBytesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String = "", //xs:base64Binary
    )

    // ---------------
    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringBoolean", xmlnsAMVi, amvi)
    class GetMessageBytesJavaLangStringBoolean(
        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAMVi, amvi)
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringBooleanResponse", xmlnsAMVi, amvi)
    class GetMessageBytesJavaLangStringBooleanResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String = "", //xs:base64Binary
    )

    // ---------------
    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringIntBoolean", xmlnsAMVi, amvi)
    class GetMessageBytesJavaLangStringIntBoolean(
        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAMVi, amvi)
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("version", xmlnsAMVi, amvi)
        val version: Int,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessageBytesJavaLangStringIntBooleanResponse", xmlnsAMVi, amvi)
    class GetMessageBytesJavaLangStringIntBooleanResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String = "", //xs:base64Binary
    )

    // ---------------
    @Serializable
    @XmlSerialName("getMessageList", xmlnsAMVi, amvi)
    class GetMessageList(
        @XmlElement(true)
        @XmlSerialName("filter", xmlnsAMVi, amvi)
        val filter: AdapterFilter,
        @XmlElement(true)
        @XmlSerialName("maxMessages", xmlnsAMVi, amvi)
        val maxMessages: Int? = null,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessageListResponse", xmlnsAMVi, amvi)
    class GetMessageListResponse(
        // имя не в XML
        val Resp: Response,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getMessagesByIDs", xmlnsAMVi, amvi)
    class GetMessagesByIDs(
        @XmlElement(true)
        @XmlSerialName("messageIds", xmlnsAMVi, amvi)
        val messageIds: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("referenceIds", xmlnsAMVi, amvi)
        val referenceIds: ArrayOfStrings = ArrayOfStrings(),
        @XmlElement(true)
        @XmlSerialName("correlationIds", xmlnsAMVi, amvi)
        val correlationIds: ArrayOfStrings = ArrayOfStrings(),
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessagesByIDsResponse", xmlnsAMVi, amvi)
    class GetMessagesByIDsResponse(
        val Resp: Response,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getMessagesByKeys", xmlnsAMVi, amvi)
    class GetMessagesByKeys(
        @XmlElement(true)
        @XmlSerialName("filter", xmlnsAMVi, amvi)
        val filter: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("maxMessages", xmlnsAMVi, amvi)
        val maxMessages: Int? = null,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessagesByKeysResponse", xmlnsAMVi, amvi)
    class GetMessagesByKeysResponse(
        val Resp: Response,
    )
// ---------------

    @Serializable
    @XmlSerialName("getMessagesWithSuccessors", xmlnsAMVi, amvi)
    class GetMessagesWithSuccessors(
        @XmlElement(true)
        @XmlSerialName("messageIds", xmlnsAMVi, amvi)
        val messageIds: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean = false,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getMessagesWithSuccessorsResponse", xmlnsAMVi, amvi)
    class GetMessagesWithSuccessorsResponse(
        val Resp: Response,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getParties", xmlnsAMVi, amvi)
    class GetParties : ComposeSOAP()

    @Serializable
    @XmlSerialName("getPartiesResponse", xmlnsAMVi, amvi)
    class GetPartiesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getPredecessorMessageId", xmlnsAMVi, amvi)
    class GetPredecessorMessageId(
        @XmlElement(true)
        @XmlSerialName("messageId", xmlnsAMVi, amvi)
        val messageIds: String,
        @XmlElement(true)
        @XmlSerialName("direction", xmlnsAMVi, amvi)
        val direction: String,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getPredecessorMessageIdResponse", xmlnsAMVi, amvi)
    class GetPredecessorMessageIdResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    ) : ComposeSOAP()

    // ---------------
    @Serializable
    @XmlSerialName("getServices", xmlnsAMVi, amvi)
    class GetServices : ComposeSOAP()

    @Serializable
    @XmlSerialName("getServicesResponse", xmlnsAMVi, amvi)
    class GetServicesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: String?,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getStatusDetails", xmlnsAMVi, amvi)
    class GetStatusDetails(
        @XmlElement(true)
        @XmlSerialName("errorCodes", xmlnsAMVi, amvi)
        val errorCodes: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("locale", xmlnsAMVi, amvi)
        val locale: LocaleAMM,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getStatusDetailsResponse", xmlnsAMVi, amvi)
    class GetStatusDetailsResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Response: ArrayOfStatusDetail,
    )
// ---------------

    @Serializable
    @XmlSerialName("getUserDefinedSearchAttributes", xmlnsAMVi, amvi)
    class GetUserDefinedSearchAttributes(
        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAMVi, amvi)
        val messageKey: String,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAMVi, amvi)
        val archive: Boolean,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchAttributesResponse", xmlnsAMVi, amvi)
    class GetUserDefinedSearchAttributesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Resp: ArrayOfBusinessAttribute,
    )

    // ---------------
    @Serializable
    @XmlSerialName("getUserDefinedSearchExtractors", xmlnsAMVi, amvi)
    class GetUserDefinedSearchExtractors(
        @XmlElement(true)
        @XmlSerialName("name", xmlnsAMVi, amvi)
        val name: String,
        @XmlElement(true)
        @XmlSerialName("namespace", xmlnsAMVi, amvi)
        val namespace: String,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchExtractorsResponse", xmlnsAMVi, amvi)
    class GetUserDefinedSearchExtractorsResponse(
        @XmlElement(true)
        val Response: _RplResponse,
    ) {
        @Serializable
        @XmlSerialName("Response", xmlnsAMVi, amvi)
        class _RplResponse(
            val AttributeMetadata: List<AttributeMetadata>,
        )
    }

    // ---------------
    @Serializable
    @XmlSerialName("getUserDefinedSearchFilters", xmlnsAMVi, amvi)
    class GetUserDefinedSearchFilters : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchFiltersResponse", xmlnsAMVi, amvi)
    class GetUserDefinedSearchFiltersResponse(
        @XmlElement(true)
        val Response: _RplResponse,
    ) {
        @Serializable
        @XmlSerialName("Response", xmlnsAMVi, amvi)
        class _RplResponse(
            val MessageInterface: List<MessageInterface>,
        )
    }

    // ---------------
    @Serializable
    @XmlSerialName("getUserDefinedSearchMessages", xmlnsAMVi, amvi)
    class GetUserDefinedSearchMessages(
        @XmlElement(true)
        @XmlSerialName("filter", xmlnsAMVi, amvi)
        val filter: AdapterFilter,
        @XmlElement(true)
        @XmlSerialName("maxMessages", xmlnsAMVi, amvi)
        val maxMessages: Int? = null,
        @XmlElement(true)
        @XmlSerialName("attributes", xmlnsAMVi, amvi)
        val attributes: ArrayOfBusinessAttribute,
        @XmlElement(true)
        @XmlSerialName("operator", xmlnsAMVi, amvi)
        val operator: String,   //AND, OR
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("getUserDefinedSearchMessagesResponse", xmlnsAMVi, amvi)
    class GetUserDefinedSearchMessagesResponse(
        // имя не в XML
        val Resp: Response,
    )

    // ---------------
    @Serializable
    @XmlSerialName("resendMessages", xmlnsAMVi, amvi)
    class ResendMessages(
        @XmlElement(true)
        @XmlSerialName("messageKeys", xmlnsAMVi, amvi)
        val messageKeys: ArrayOfStrings,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("resendMessagesResponse", xmlnsAMVi, amvi)
    class ResendMessagesResponse(
        @XmlElement(true)
        @XmlSerialName("Response", "", "")
        val Resp: AdminActionResultMap,
    )

    // ---------------
    @Serializable
    class LocaleAMM(
        @XmlElement(true)
        @XmlSerialName("language", xmlnsAfw, afwp)
        val language: String? = null,
        @XmlElement(true)
        @XmlSerialName("country", xmlnsAfw, afwp)
        val country: String? = null,
        @XmlElement(true)
        @XmlSerialName("variant", xmlnsAfw, afwp)
        val variant: String? = null,
    )

    @Serializable
    class ArrayOfStatusDetail(
        @XmlSerialName("StatusDetail", xmlnsAfw, afwp)
        val list: List<StatusDetail> = listOf(),
    )

    @Serializable
    class StatusDetail(
        @XmlElement(true)
        @XmlSerialName("errorLabelID", xmlnsAfw, afwp)
        val errorLabelID: String? = null,
        @XmlElement(true)
        @XmlSerialName("description", xmlnsAfw, afwp)
        val description: String? = null,
        @XmlElement(true)
        @XmlSerialName("text", xmlnsAfw, afwp)
        val text: String? = null,
    )

    @Serializable
    class AdapterFilter(
        // см.file2.wsdl AdapterFilter
        // все поля сделаны изменяемыми для удобства кода
        @XmlElement(true)
        @XmlSerialName("applicationComponent", xmlnsAfw, afwp)
        var applicationComponent: String? = null,
        @XmlElement(true)
        @XmlSerialName("archive", xmlnsAfw, afwp)
        var archive: Boolean = false,

        @XmlElement(true)
        @XmlSerialName("connectionName", xmlnsAfw, afwp)
        var connectionName: String? = null,

        @XmlElement(true)
        @XmlSerialName("correlationID", xmlnsAfw, afwp)
        var correlationID: String? = null,

        @XmlElement(true)
        @XmlSerialName("dateType", xmlnsAfw, afwp)
        var dateType: Int = 0,

        @XmlElement(true)
        @XmlSerialName("direction", xmlnsAfw, afwp)
        var direction: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCategory", xmlnsAfw, afwp)
        var errorCategory: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCode", xmlnsAfw, afwp)
        var errorCode: String? = null,

        @XmlElement(true)
        @XmlSerialName("fromTime", xmlnsAfw, afwp)
        var fromTime: String? = null,   //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("interface", xmlnsAfw, afwp)
        var interface_: rn2Interface? = null,

        @XmlElement(true)
        @XmlSerialName("messageFormat", xmlnsAfw, afwp)
        var messageFormat: String? = null,

        @XmlElement(true)
        @XmlSerialName("messageIDs", xmlnsAfw, afwp)
        var messageIDs: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("messageType", xmlnsAfw, afwp)
        var messageType: String? = null,

        @XmlElement(true)
        @XmlSerialName("nodeId", xmlnsAfw, afwp)
        var nodeId: Long = 0,

        @XmlElement(true)
        @XmlSerialName("onlyFaultyMessages", xmlnsAfw, afwp)
        var onlyFaultyMessages: Boolean = false,

        @XmlElement(true)
        @XmlSerialName("persistUntil", xmlnsAfw, afwp)
        var persistUntil: String? = null,   //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("protocol", xmlnsAfw, afwp)
        var protocol: String? = null,

        @XmlElement(true)
        @XmlSerialName("qualityOfService", xmlnsAfw, afwp)
        var qualityOfService: String? = null,

        @XmlElement(true)
        @XmlSerialName("receiverInterface", xmlnsAfw, afwp)
        var receiverInterface: rn2Interface? = null,

        @XmlElement(true)
        @XmlSerialName("receiverName", xmlnsAfw, afwp)
        var receiverName: String? = null,

        @XmlSerialName("receiverParty", xmlnsAfw, afwp)
        var receiverParty: rn2Party? = null,

        @XmlElement(true)
        @XmlSerialName("referenceIDs", xmlnsAfw, afwp)
        var referenceIDs: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("retries", xmlnsAfw, afwp)
        var retries: Int = 0,

        @XmlElement(true)
        @XmlSerialName("retryInterval", xmlnsAfw, afwp)
        var retryInterval: Long = 0,

        @XmlElement(true)
        @XmlSerialName("scheduleTime", xmlnsAfw, afwp)
        var scheduleTime: String? = null,   //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("senderInterface", xmlnsAfw, afwp)
        var senderInterface: rn2Interface? = null,

        @XmlElement(true)
        @XmlSerialName("senderName", xmlnsAfw, afwp)
        var senderName: String? = null,

        @XmlSerialName("senderParty", xmlnsAfw, afwp)
        var senderParty: rn2Party? = null,

        @XmlElement(true)
        @XmlSerialName("sequenceID", xmlnsAfw, afwp)
        var sequenceID: String? = null,

        @XmlElement(true)
        @XmlSerialName("serializationContext", xmlnsAfw, afwp)
        var serializationContext: String? = null,

        @XmlElement(true)
        @XmlSerialName("serviceDefinition", xmlnsAfw, afwp)
        var serviceDefinition: String? = null,

        @XmlElement(true)
        @XmlSerialName("softwareComponent", xmlnsAfw, afwp)
        var softwareComponent: String? = null,

        @XmlElement(true)
        @XmlSerialName("status", xmlnsAfw, afwp)
        var status: String? = null,

        @XmlElement(true)
        @XmlSerialName("statuses", xmlnsAfw, afwp)
        var statuses: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("timesFailed", xmlnsAfw, afwp)
        var timesFailed: Int = 0,

        @XmlElement(true)
        @XmlSerialName("toTime", xmlnsAfw, afwp)
        var toTime: String? = null, //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("transport", xmlnsAfw, afwp)
        var transport: String? = null,

        @XmlElement(true)
        @XmlSerialName("validUntil", xmlnsAfw, afwp)
        var validUntil: String? = null, //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("wasEdited", xmlnsAfw, afwp)
        var wasEdited: Boolean = false,

        @XmlElement(true)
        @XmlSerialName("scenarioIDs", xmlnsAfw, afwp)
        var scenarioIDs: ArrayOfStrings? = null,

        @XmlElement(true)
        @XmlSerialName("rootID", xmlnsAfw, afwp)
        var rootID: String? = null,

        @XmlElement(true)
        @XmlSerialName("returnLogLocations", xmlnsAfw, afwp)
        var returnLogLocations: Boolean? = null,

        @XmlElement(true)
        @XmlSerialName("onlyLogLocationsWithPayload", xmlnsAfw, afwp)
        var onlyLogLocationsWithPayload: Boolean? = null,
    )

    @Serializable
    @XmlSerialName("Response", "", "")
    class Response(
        @XmlElement(true)
        @XmlSerialName("date", xmlnsAfw, afwp)
        val date: String = "",  //в 7.4 его нет
        @XmlElement(true)
        val afw: LAFW? = null,
        @XmlElement(true)
        @XmlSerialName("number", xmlnsAfw, afwp)
        val number: Int,
        @XmlElement(true)
        @XmlSerialName("warning", xmlnsAfw, afwp)
        val warning: Boolean,
        @XmlElement(true)
        @XmlSerialName("displayPermissionWarning", xmlnsAfw, afwp)
        val displayPermissionWarning: Boolean,
    )

    @Serializable
    @XmlSerialName("list", xmlnsAfw, afwp)
    class LAFW(val list: List<AdapterFrameworkData>)

    @Serializable
    @XmlSerialName("AdapterFrameworkData", xmlnsAfw, afwp)
    class AdapterFrameworkData(
        // см. file2.wsdl для complexType name="AdapterFrameworkData", почти все поля необязательные
        @XmlElement(true)
        @XmlSerialName("applicationComponent", xmlnsAfw, afwp)
        val applicationComponent: String? = null,

        @XmlElement(true)
        @XmlSerialName("businessMessage", xmlnsAfw, afwp)
        val businessMessage: rn2Boolean? = null,

        @XmlElement(true)
        @XmlSerialName("cancelable", xmlnsAfw, afwp)
        val cancelable: rn2Boolean? = null, // = rn2Boolean(false),

        @XmlElement(true)
        @XmlSerialName("connectionName", xmlnsAfw, afwp)
        val connectionName: String? = null,

        @XmlElement(true)
        @XmlSerialName("credential", xmlnsAfw, afwp)
        val credential: String? = null,

        @XmlElement(true)
        @XmlSerialName("direction", xmlnsAfw, afwp)
        val direction: MPI.DIRECTION? = null, // EnumDirection.OUTBOUND,

        @XmlSerialName("editable", xmlnsAfw, afwp)
        val editable: rn2Boolean? = null, // = rn2Boolean(false),

        @XmlElement(true)
        @XmlSerialName("endTime", xmlnsAfw, afwp)
        val endTime: String? = null,    //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("endpoint", xmlnsAfw, afwp)
        val endpoint: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCategory", xmlnsAfw, afwp)
        val errorCategory: String? = null,

        @XmlElement(true)
        @XmlSerialName("errorCode", xmlnsAfw, afwp)
        val errorCode: String? = null,

        @XmlElement(true)
        @XmlSerialName("headers", xmlnsAfw, afwp)
        val headers: String? = null,

        @XmlSerialName("interface", xmlnsAfw, afwp)
        val interface_: rn2Interface? = null, //rn2Interface("a", "urn:a"),

        @XmlElement(true)
        @XmlSerialName("isPersistent", xmlnsAfw, afwp)
        val isPersistent: Boolean,

        @XmlElement(true)
        @XmlSerialName("messageID", xmlnsAfw, afwp)
        val messageID: String?,

        @XmlElement(true)
        @XmlSerialName("messageKey", xmlnsAfw, afwp)
        val messageKey: String?,

        @XmlElement(true)
        @XmlSerialName("messageType", xmlnsAfw, afwp)
        val messageType: String?,

        @XmlElement(true)
        @XmlSerialName("nodeId", xmlnsAfw, afwp)
        val nodeId: Int,

        @XmlElement(true)
        @XmlSerialName("persistUntil", xmlnsAfw, afwp)
        val persistUntil: String?,  //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("protocol", xmlnsAfw, afwp)
        val protocol: String?,

        @XmlElement(true)
        @XmlSerialName("qualityOfService", xmlnsAfw, afwp)
        val qualityOfService: String?,

        @XmlSerialName("receiverInterface", xmlnsAfw, afwp)
        val receiverInterface: rn2Interface?, // = rn2Interface("SI_Sync", "urn:"),

        @XmlElement(true)
        @XmlSerialName("receiverName", xmlnsAfw, afwp)
        val receiverName: String?,

        @XmlSerialName("receiverParty", xmlnsAfw, afwp)
        val receiverParty: rn2Party?, // = rn2Party("http://sap.com/xi/XI", "", "XIParty"),

        @XmlElement(true)
        @XmlSerialName("referenceID", xmlnsAfw, afwp)
        val referenceID: String?,

        @XmlSerialName("restartable", xmlnsAfw, afwp)
        val restartable: rn2Boolean?, // = rn2Boolean(false),

        @XmlElement(true)
        @XmlSerialName("retries", xmlnsAfw, afwp)
        val retries: Int,

        @XmlElement(true)
        @XmlSerialName("retryInterval", xmlnsAfw, afwp)
        val retryInterval: Long = 0,

        @XmlElement(true)
        @XmlSerialName("scheduleTime", xmlnsAfw, afwp)
        val scheduleTime: String?,  //xs:dateTime

        @XmlSerialName("senderInterface", xmlnsAfw, afwp)
        val senderInterface: rn2Interface?, // = rn2Interface("SI_Sync", "urn:"),

        @XmlElement(true)
        @XmlSerialName("senderName", xmlnsAfw, afwp)
        val senderName: String?,

        @XmlSerialName("senderParty", xmlnsAfw, afwp)
        val senderParty: rn2Party?, // = rn2Party("http://sap.com/xi/XI", "", "XIParty"),

        @XmlElement(true)
        @XmlSerialName("sequenceNumber", xmlnsAfw, afwp)
        val sequenceNumber: Long?,

        @XmlElement(true)
        @XmlSerialName("serializationContext", xmlnsAfw, afwp)
        val serializationContext: String?,

        @XmlElement(true)
        @XmlSerialName("serviceDefinition", xmlnsAfw, afwp)
        val serviceDefinition: String?,

        @XmlElement(true)
        @XmlSerialName("softwareComponent", xmlnsAfw, afwp)
        val softwareComponent: String?,

        @XmlElement(true)
        @XmlSerialName("startTime", xmlnsAfw, afwp)
        val startTime: String?, //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("status", xmlnsAfw, afwp)
        val status: String?,

        @XmlElement(true)
        @XmlSerialName("timesFailed", xmlnsAfw, afwp)
        val timesFailed: Long,

        @XmlElement(true)
        @XmlSerialName("transport", xmlnsAfw, afwp)
        val transport: String?,

        @XmlElement(true)
        @XmlSerialName("validUntil", xmlnsAfw, afwp)
        val validUntil: String?,    //xs:dateTime

        @XmlElement(true)
        @XmlSerialName("version", xmlnsAfw, afwp)
        val version: String?,

        @XmlSerialName("wasEdited", xmlnsAfw, afwp)
        val wasEdited: rn2Boolean?, // = rn2Boolean(false),

        @XmlSerialName("businessAttributes", xmlnsAfw, afwp)
        val businessAttributes: ArrayOfBusinessAttribute?,

        @XmlElement(true)
        @XmlSerialName("payloadPermissionWarning", xmlnsAfw, afwp)
        val payloadPermissionWarning: rn2Boolean?,

        @XmlElement(true)
        @XmlSerialName("errorLabel", xmlnsAfw, afwp)
        val errorLabel: Int,

        @XmlElement(true)
        @XmlSerialName("scenarioIdentifier", xmlnsAfw, afwp)
        val scenarioIdentifier: String?,

        @XmlElement(true)
        @XmlSerialName("parentID", xmlnsAfw, afwp)
        val parentID: String?,

        @XmlSerialName("duration", xmlnsAfw, afwp)
        val duration: rn2Duration?, // = rn2Duration(0),

        @XmlElement(true)
        @XmlSerialName("size", xmlnsAfw, afwp)
        val size: Long?,

        @XmlElement(true)
        @XmlSerialName("messagePriority", xmlnsAfw, afwp)
        val messagePriority: Long?,

        @XmlElement(true)
        @XmlSerialName("rootID", xmlnsAfw, afwp)
        val rootID: String?,

        @XmlElement(true)
        @XmlSerialName("sequenceID", xmlnsAfw, afwp)
        val sequenceID: String?, //CPQQUOTE_QUEUE

        @XmlElement(true)
        @XmlSerialName("logLocations", xmlnsAfw, afwp)
        val logLocations: ArrayOfStrings?,

        @XmlElement(true)
        @XmlSerialName("passport", xmlnsAfw, afwp)
        val passport: String?,//2A54482A0300E60000000000000000000000000000000000000002A54482A

        @XmlElement(true)
        @XmlSerialName("passportTID", xmlnsAfw, afwp)
        val passportTID: String?,//005056bf-8d88-1eed-84ae-893f641f6670
    )

    @Serializable
    class AdminActionResultMap(
        @XmlElement(true)
        @XmlSerialName("keyList", xmlnsAfw, afwp)
        val keyList: ArrayOfStrings,
        @XmlElement(true)
        @XmlSerialName("resultList", xmlnsAfw, afwp)
        val resultList: AdminActionResultList,
    )

    @Serializable
    class AdminActionResultList(
        @XmlElement(true)
        @XmlSerialName("AdminActionResult", xmlnsAfw, afwp)
        val list: List<AdminActionResult>,
    )

    @Serializable
    class AdminActionResult(
        @XmlElement(true)
        @XmlSerialName("resultCode", xmlnsAfw, afwp)
        val resultCode: String?,
        @XmlElement(true)
        @XmlSerialName("resultText", xmlnsAfw, afwp)
        val resultText: String?,
        @XmlElement(true)
        @XmlSerialName("successful", xmlnsAfw, afwp)
        val successful: Boolean,
    )

    @Serializable
    class ArrayOfBusinessAttribute(
        @XmlElement(true)
        val list: List<BusinessAttribute> = listOf(),
    )

    @Serializable
    @XmlSerialName("BusinessAttribute", xmlnsAfw, afwp)
    class BusinessAttribute(
        @XmlElement(true)
        @XmlSerialName("name", xmlnsAfw, afwp)
        val name: String,
        @XmlElement(true)
        @XmlSerialName("value", xmlnsAfw, afwp)
        val value: String,
    )

    @Serializable
    @XmlSerialName("MessageInterface", "urn:com.sap.aii.mdt.api.data", "rn2")
    class MessageInterface(
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
    @XmlSerialName("AttributeMetadata", xmlnsAfw, afwp)
    class AttributeMetadata(
        @XmlElement(true)
        @XmlSerialName("name", xmlnsAfw, afwp)
        val name: String,
        @XmlElement(true)
        @XmlSerialName("description", xmlnsAfw, afwp)
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
        val strings: List<String> = listOf(),
    ) {
        constructor(vararg s: String) : this(s.toList())
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

    // -----------------------------------------------------------------------------------------------
    @Serializable
    @XmlSerialName("getProfiles", xmlnsPPVi, ppvi)
    class GetProfilesRequest(
        @XmlSerialName("applicationKey", xmlnsPPVi, ppvi)
        @XmlElement val applicationKey: String,
        @XmlSerialName("active", xmlnsPPVi, ppvi)
        @XmlElement val active: Boolean,
    )
    @Serializable
    @XmlSerialName("getProfilesResponse", xmlnsPPVi, ppvi)
    class GetProfilesResponse(
        @XmlElement val response: PPResponse
    ): ComposeSOAP()

    @Serializable
    @XmlSerialName("Response", "", "")
    class PPResponse(
        @XmlElement val wsProfile: WSProfile
    )
    @Serializable
    @XmlSerialName("WSProfile", xmlnsStat, stat)
    class WSProfile(
        @XmlSerialName("activation", xmlnsStat, stat)
        @XmlElement val activation: String,
        @XmlSerialName("applicationKey", xmlnsStat, stat)
        @XmlElement val applicationKey: String,
        @XmlSerialName("profileKey", xmlnsStat, stat)
        @XmlElement val profileKey: String,
    )
}