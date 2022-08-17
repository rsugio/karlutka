package karlutka.serialization

import karlutka.parsers.pi.AdapterMessageMonitoringVi
import karlutka.parsers.pi.IChannelAdmin
import karlutka.parsers.pi.SAPControl
import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

class KSoap {
    @Serializable
    open class ComposeSOAP {
        fun composeSOAP() = xmlserializer.encodeToString(Envelope(null, Envelope.Body(this)))
    }

    @Serializable
    @XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV")
    class Envelope<BODYTYPE>(
        @XmlElement(true)
        @Contextual
        @XmlSerialName("Header", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV")
        val header: CompactFragment? = null,    // пока только для парсинга
        val body: Body<BODYTYPE>,
    ) {
        @Serializable
        @XmlSerialName("Body", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV")
        class Body<BODYTYPE>(
            @Polymorphic
            val data: BODYTYPE?,
            @XmlElement(true)
            val fault: Fault? = null,           // только для парсинга
        )
    }

    @Serializable
    @XmlSerialName("Fault", "http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV")
    data class Fault(
        @XmlElement(true)
        @XmlSerialName("faultcode", "", "")
        var faultcode: String = "",
        @XmlElement(true)
        @XmlSerialName("faultstring", "", "")
        var faultstring: String = "",
        @XmlElement(true)
        @XmlSerialName("detail", "", "")
        @Contextual
        private val detail: CompactFragment? = null,
    ) {
        fun isSuccess() = faultcode == ""
        fun isFailure() = faultcode.isNotBlank()
    }


    companion object {
        val xmlmodule = SerializersModule {
            polymorphic(Any::class) {
                subclass(Fault::class, serializer())
                subclass(AdapterMessageMonitoringVi.CancelMessages::class, serializer())
                subclass(AdapterMessageMonitoringVi.CancelMessagesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.FailEoioMessage::class, serializer())
                subclass(AdapterMessageMonitoringVi.FailEoioMessageResponse::class, serializer())

                subclass(AdapterMessageMonitoringVi.GetAllAvailableStatusDetails::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetAllAvailableStatusDetailsResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetConnections::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetConnectionsResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetErrorCodes::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetErrorCodesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetIntegrationFlows::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetIntegrationFlowResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetInterfaces::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetInterfacesResponse::class, serializer())

                subclass(AdapterMessageMonitoringVi.GetLogEntries::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetLogEntriesResponse::class, serializer())
                // https://blogs.sap.com/2017/10/02/sap-pi-message-staging-and-logging-issue-aae-message-payload-retrieval/
                subclass(AdapterMessageMonitoringVi.GetLoggedMessageBytes::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetLoggedMessageBytesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessageBytesJavaLangStringBoolean::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessageBytesJavaLangStringBooleanResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessageBytesJavaLangStringIntBoolean::class, serializer())
                subclass(
                    AdapterMessageMonitoringVi.GetMessageBytesJavaLangStringIntBooleanResponse::class,
                    serializer()
                )
                subclass(AdapterMessageMonitoringVi.GetMessageList::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessageListResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessagesByIDs::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessagesByIDsResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessagesByKeys::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessagesByKeysResponse::class, serializer())

                subclass(AdapterMessageMonitoringVi.GetMessagesWithSuccessors::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetMessagesWithSuccessorsResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetParties::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetPartiesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetPredecessorMessageId::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetPredecessorMessageIdResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetServices::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetServicesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetStatusDetails::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetStatusDetailsResponse::class, serializer())

                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchAttributes::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchAttributesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchExtractors::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchExtractorsResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchFilters::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchFiltersResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchMessages::class, serializer())
                subclass(AdapterMessageMonitoringVi.GetUserDefinedSearchMessagesResponse::class, serializer())
                subclass(AdapterMessageMonitoringVi.ResendMessages::class, serializer())
                subclass(AdapterMessageMonitoringVi.ResendMessagesResponse::class, serializer())

                subclass(IChannelAdmin.StartChannels::class, serializer())
                subclass(IChannelAdmin.StopChannels::class, serializer())
//            subclass(SetChannelAutomationStatus::class, serializer())
//            subclass(GetChannelAutomationStatus::class, serializer())
                subclass(IChannelAdmin.GetChannelAdminHistory::class, serializer())

                subclass(SAPControl.ListLogFiles::class, serializer())
                subclass(SAPControl.ListLogFilesResponse::class, serializer())
                subclass(SAPControl.ReadLogFile::class, serializer())
                subclass(SAPControl.ReadLogFileResponse::class, serializer())
            }
        }
        val xmlserializer = XML(xmlmodule) {
            autoPolymorphic = true
        }


    }
}