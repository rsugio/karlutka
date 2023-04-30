package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class XIAdapterEngineRegistration {
    @Serializable
    @XmlSerialName("scenario", "", "")
    class Scenario(
        @XmlElement(true) val scenname: String = "",
        @XmlElement(true) val scenversion: String = "",
        @XmlElement(true) val sceninst: String = "",
        @XmlElement(true) val component: MutableList<Component> = mutableListOf(),
    ) {
        fun encodeToString() = xmlserializer.encodeToString(this)
        fun getAction(): String? {
            return component[0].property.find { it.propname == "action" }?.propvalue
        }
    }

    @Serializable
    @XmlSerialName("component", "", "")
    class Component(
        @XmlElement(true) var compname: String,
        @XmlElement(true) var compversion: String? = null,
        @XmlElement(true) var comphost: String? = null,
        @XmlElement(true) var compinst: String? = null,
        var messages: Messages? = null,
        @XmlElement(true) @XmlSerialName("property", "", "") val property: MutableList<Property> = mutableListOf(),
    ) {
        fun addProperty(name: String, value: String) {
            property.add(Property(name, value))
        }

        fun addProperties(m: Map<String, String>) {
            m.forEach { (k, v) ->
                property.add(Property(k, v))
            }
        }
    }

    @Serializable
    @XmlSerialName("messages", "", "")
    class Messages(
        @XmlElement(true) @XmlSerialName("message", "", "") val message: MutableList<Message> = mutableListOf(),
    )

    @Serializable
    class Message(
        @XmlElement(true) val messalert: String,
        @XmlElement(true) val messseverity: String,
        @XmlElement(true) val messarea: String,
        @XmlElement(true) val messnumber: String,
        @XmlElement(true) val messparam1: String,
        @XmlElement(true) val messparam2: String,
        @XmlElement(true) val messparam3: String,
        @XmlElement(true) val messparam4: String,
        @XmlElement(true) val messtext: String,
    )

    @Serializable
    class Property(
        @XmlElement(true) val propname: String,
        @XmlElement(true) val propvalue: String,
    )

    class RegisterAppWithSLD() {
        fun answer(): Scenario {
            val s = Scenario()
            val c = Component("ActivityLogEntry")
            c.addProperties(mapOf("Message" to "Registration of Adapter Framework with SLD was successful", "Type" to "INFO"))
            s.component.add(c)
            return s
        }
    }

    class GetApplicationDetailsFromSLD() {
        fun answer(afname: String, hostname: String, cacherefresh: String): Scenario {
            val s = Scenario()
            val SLDData = Component("SLDData")
            SLDData.addProperties(
                mapOf(
                    "AdapterFramework.SldInstanceName" to afname,
                    "AdapterFramework.Caption" to "Adapter Engine on $afname",
                    "AdapterFramework.HostName" to hostname,
                    "AdapterFramework.TechnicalHost.HostName" to hostname,
                    "XIDomain.IntegrationServer.MsgUrl" to "http://$hostname:80/XI",
                    "AdapterFramework.HttpPort" to "80",
                    "AdapterFramework.TechnicalHost.HttpPort" to "80",
                    "AdapterFramework.IsCentral" to "false",
                    "AdapterFramework.TechnicalHost.SystemName" to "TST",
                    "AdapterFramework.SAPSystemName" to "TST",
                    "AdapterFramework.ApplicationId" to "af",
                    "AdapterFramework.LogicalSystemName" to "af",
                    "AdapterFramework.TechnicalRoleId" to "J2EE_SERVER",

                    "RemoteAdminService.CacheRefresh.Purpose" to "CacheRefresh",
                    "RemoteAdminService.CacheRefresh.Url" to "http://$hostname:80/CPACache/invalidate",
                    "RemoteAdminService.CacheRefresh.Protocol" to "http",
                    "RemoteAdminService.CacheRefresh.SldInstanceName" to "CacheRefresh.$afname",
                    "RemoteAdminService.CacheRefresh.SldClassName" to "SAP_XIRemoteAdminService",
                    "RemoteAdminService.CacheRefresh.SldCaption" to "CacheRefresh.$afname",

                    "RemoteAdminService.AdminTool.Purpose" to "AdminTool",
                    "RemoteAdminService.AdminTool.Url" to "http://$hostname:80/mdt",
                    "RemoteAdminService.AdminTool.Protocol" to "http",
                    "RemoteAdminService.AdminTool.SldInstanceName" to "AdminTool.$afname",
                    "RemoteAdminService.AdminTool.SldClassName" to "SAP_XIRemoteAdminService",
                    "RemoteAdminService.AdminTool.SldCaption" to "AdminTool.$afname",

                    "RemoteAdminService.RuntimeCheck.Purpose" to "RuntimeCheck",
                    "RemoteAdminService.RuntimeCheck.Url" to "http://$hostname:80/AdapterFramework/rtc",
                    "RemoteAdminService.RuntimeCheck.Protocol" to "http",
                    "RemoteAdminService.RuntimeCheck.SldInstanceName" to "RuntimeCheck.$afname",
                    "RemoteAdminService.RuntimeCheck.SldClassName" to "SAP_XIRemoteAdminService",
                    "RemoteAdminService.RuntimeCheck.SldCaption" to "RuntimeCheck.$afname",
                )
            )
            s.component.add(SLDData)
            val SLDStatus = Component("SLDStatus")
            SLDStatus.addProperty("result", "OK")
            s.component.add(SLDStatus)

            s.component.add(featureCheck("Adapter Framework reports registration status", "completely registered"))
            s.component.add(featureCheck("CPA cache update URL used by Adapter Engine", cacherefresh))
            s.component.add(featureCheck("CPA cache update URL stored in SLD", cacherefresh))
            s.component.add(featureCheck("CPA cache update URL", "URL currently used by Adapter Engine and URL stored in SLD are the same"))
            s.component.add(featureCheck("Messaging URL of Integration Server used by Adapter Engine", "http://$hostname/XI"))
            s.component.add(featureCheck("Messaging URL of Integration Server stored in SLD", "http://$hostname/XI"))
            s.component.add(featureCheck("Messaging URL of Integration Server", "URLs are the same"))
            return s
        }
    }


    companion object {
        private val xmlmodule = SerializersModule {}
        private val xmlserializer = XML(xmlmodule) {
            autoPolymorphic = false
        }

        fun decodeFromString(s: String) = xmlserializer.decodeFromString<Scenario>(s)

        fun featureCheck(name: String, descr: String, result: String = "OK", details: String = ""): Component {
            return Component(
                "featureCheck", null, null, null, null, mutableListOf(
                    Property("feature", name),
                    Property("featureDescr", descr),
                    Property("feature", result),
                    Property("feature", details),
                )
            )
        }
    }
}