package karlutka.parsers.pi

import karlutka.serialization.KSoap.Companion.xmlserializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// /remoteapi == http://host:50000/remoteapi/index.html
// статусные сообщения, так себе http://host:50000/remoteapi/getstatuslogs?channelName=qqq&service=BC_TEST1&party=&adapterType=REST
// пинг канала, ценная вещь http://host:50000/remoteapi/channelselftest?channel=qqqq&service=BC_TEST1&party=
// это всё com.sap.aii.monitoring.remoteapi
class RemoteApi {
    @Serializable
    @XmlSerialName("scenario", "", "")
    class Scenario(
        // результат {{host}}/remoteapi/channelselftest?channel=qqqq&service=BC_TEST1
        @XmlElement(true)
        val scenname: String = "",
        @XmlElement(true)
        val scenversion: String = "",
        @XmlElement(true)
        val sceninst: String = "",
        @XmlElement(true)
        val component: Component,
    ) {
        companion object {
            fun parse(sxml: String) = xmlserializer.decodeFromString<Scenario>(sxml)
        }
    }

    @Serializable
    @XmlSerialName("component", "", "")
    class Component(
        @XmlElement(true)
        val compname: String = "",
        @XmlElement(true)
        val comphost: String = "",
        @XmlElement(true)
        val compversion: String = "",
        @XmlElement(true)
        val property: List<Property> = listOf(),
    )

    @Serializable
    @XmlSerialName("property", "", "")
    class Property(
        @XmlElement(true)
        val propname: String,
        @XmlElement(true)
        val propvalue: String,
    )
}