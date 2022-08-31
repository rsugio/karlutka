package karlutka.parsers.pi

import karlutka.serialization.KSoap
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// http://host:50000/ChannelAdminService/ChannelAdmin
// Content-Type: строго text/xml. Если application/soap+xml то другой неймспейс
// штука полезная только для старта/стопа и просмотра истории запусков, в остальных случаях бесмыссленная
// на 2022-08-13: не протестировано
class IChannelAdmin {
    @Serializable
    @XmlSerialName("startChannels", "urn:com:sap:netweaver:pi:monitoring", "ica")
    class StartChannels(
        @XmlElement(true)
        val channel: List<ChannelAdminDescriptor> = listOf(),
        @XmlElement(true)
        @XmlSerialName("language", "", "")
        val language: String? = null,
    ) : KSoap.ComposeSOAP()

    @Serializable
    @XmlSerialName("channel", "", "")
    class ChannelAdminDescriptor(
        val name: String,
        val service: String,
        val party: String = "",
        val status: List<ChannelAdminStatus> = listOf(),
    )

    @Serializable
    @XmlSerialName("channelAdminStatus", "", "")
    class ChannelAdminStatus(
        @XmlElement(true)
        val errorInfo: String? = null,
        @XmlElement(true)
        val automationStatus: String? = null,
        val nodeName: String? = null,
        val state: String? = null,
        val activationState: String? = null,
    )

    @Serializable
    @XmlSerialName("stopChannels", "urn:com:sap:netweaver:pi:monitoring", "ica")
    class StopChannels(
        @XmlElement(true)
        val channel: List<ChannelAdminDescriptor> = listOf(),
        @XmlElement(true)
        @XmlSerialName("language", "", "")
        val language: String? = null,
    ) : KSoap.ComposeSOAP()

    @Serializable
    @XmlSerialName("getChannelAdminHistory", "urn:com:sap:netweaver:pi:monitoring", "ica")
    class GetChannelAdminHistory(
        @XmlElement(true)
        val channels: List<ChannelAdminDescriptor> = listOf(),
        @XmlElement(true)
        @XmlSerialName("language", "", "")
        val language: String? = null,
    ) : KSoap.ComposeSOAP()
}