package karlutka.parsers.pi

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

class Hm {
    companion object {
        private val hmxml = SerializersModule {
            polymorphic(Any::class) {
                subclass(Value::class, serializer())
                subclass(Instance::class, serializer())
                subclass(String::class, String.serializer())
            }
        }
        private val hmserializer = XML(hmxml) {
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun parseInstance(sxml: String): Instance {
            return hmserializer.decodeFromString(sxml)
        }

        // возвращает инстанс завёрнутый в атрибут
        fun instance(name: String, typeid: String, vararg atts: Attribute): Attribute {
            return Attribute(
                false, null, name,
                Value(0, false, listOf(Instance(typeid, atts.toMutableList())))
            )
        }

    }

    @Serializable
    @XmlSerialName("instance", "", "")
    class Instance(
        val typeid: String,
        @XmlElement(true)
        val attribute: MutableList<Attribute> = mutableListOf(),
    ) {
        fun string(name: String): String? {
            for (a in attribute) {
                if (a.name == name && a.leave_typeid == "string")
                    if (a.value.isnull)
                        return null
                    else
                        return a.value.value[0] as String
            }
            error("Not found string: $name")
        }

        fun instance(name: String): Instance? {
            for (a in attribute) {
                if (a.name == name && a.leave_typeid == null)
                    if (a.value.isnull)
                        return null
                    else {
                        return a.value.value.find { it is Instance } as Instance?
                    }
            }
            error("Not found instance: $name")
        }
    }

    @Serializable
    @XmlSerialName("attribute", "", "")
    class Attribute(
        var isleave: Boolean = true,
        var leave_typeid: String? = null,
        var name: String,
        @XmlElement(true) val value: Value,
    ) {
        fun string(): String? {
            require(leave_typeid == "string")
            if (value.isnull)
                return null
            else
                return value.value[0] as String
        }
    }

    @Serializable
    @XmlSerialName("value", "", "")
    class Value(
        var index: Int = 0,
        var isnull: Boolean = false,
        @XmlValue(true) val value: List<@Polymorphic Any> = listOf(),
    )

    data class HmString(val s: String?) {
        fun attr(n: String) = Attribute(
            true, "string", n, if (s == null) Value(0, true)
            else Value(0, false, mutableListOf(s))
        )
    }

    data class ApplCompLevel(
        val Release: String = "7.0",
        val SupportPackage: String = "0"
    ) {
        fun attr(n: String = "ClientLevel") = instance(
            n, "com.sap.aii.util.applcomp.ApplCompLevel",
            HmString(Release).attr("Release"),
            HmString(SupportPackage).attr("SupportPackage")
        )

    }

    class HmiMethodInput(val map: LinkedHashMap<String, String>) {
        fun attr(name: String = "MethodInput"): Attribute {
            require(map.size == 1)    //TODO нужен пример на ==0 и >1
            val e = map.entries.toList()[0]
            return instance(
                name, "com.sap.aii.util.hmi.api.HmiMethodInput",
                instance(
                    "Parameters", "com.sap.aii.util.hmi.core.gdi2.EntryStringString",
                    HmString(e.key).attr("Key"),
                    HmString(e.value).attr("Value")
                )
            )
        }
    }

    data class HmiMethodOutput(val ContentType: String, val Return: String) {
        companion object {
            fun from(i: Instance?): HmiMethodOutput? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodOutput")
                return HmiMethodOutput(i.string("ContentType")!!, i.string("Return")!!)
            }
        }
    }

    data class HmiMethodFault(
        val LocalizedMessage: String,
        val Severity: String,
        val OriginalStackTrace: String?,
        val RootCauseAsString: String?,
        val Details: String?,
    ) {
        companion object {
            fun from(i: Instance?): HmiMethodFault? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodFault")
                return HmiMethodFault(
                    i.string("LocalizedMessage")!!,
                    i.string("Severity")!!,
                    i.string("OriginalStackTrace"),
                    i.string("RootCauseAsString"),
                    i.string("Details")
                )
            }
        }
    }

    data class HmiCoreException(
        val LocalizedMessage: String,
        val Severity: String,
        val OriginalStackTrace: String?,
        val SubtypeId: String?,
    ) {
        companion object {
            fun from(i: Instance?): HmiCoreException? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiCoreException")
                return HmiCoreException(
                    i.string("LocalizedMessage")!!,
                    i.string("Severity")!!,
                    i.string("OriginalStackTrace"),
                    i.string("SubtypeId")
                )
            }
        }
    }

    data class HmiRequest(
        val RequestId: String,                  //IGUID
        val RequiresSession: Boolean = false,
        val ServiceId: String, //UriElement
        val MethodId: String, //UriElement
        val HmiMethodInput: HmiMethodInput, //HmiMethodInput
        val ServerLogicalSystemName: String?,
        val ServerApplicationId: String?,
        val ClientId: String,                     //IGUID
        val ClientLevel: ApplCompLevel,
        val ClientUser: String,
        val ClientPassword: String,
        val ClientLanguage: String,
        val ControlFlag: Int = 0,
        val HmiSpecVersion: String?
    ) {
        fun instance(): Instance {
            val a = mutableListOf<Attribute>()
            a.add(HmString(RequestId).attr("RequestId"))
            a.add(HmString(RequiresSession.toString()).attr("RequiresSession"))
            a.add(HmString(ServiceId).attr("ServiceId"))
            a.add(HmString(MethodId).attr("MethodId"))
            a.add(HmiMethodInput.attr())
            if (ServerLogicalSystemName != null) a.add(HmString(ServerLogicalSystemName).attr("ServerLogicalSystemName"))
            if (ServerApplicationId != null) a.add(HmString(ServerApplicationId).attr("ServerApplicationId"))
            a.add(HmString(ClientId).attr("ClientId"))
            a.add(ClientLevel.attr())
            a.add(HmString(ClientUser).attr("ClientUser"))
            a.add(HmString(ClientPassword).attr("ClientPassword"))
            a.add(HmString(ClientLanguage).attr("ClientLanguage"))
            a.add(HmString(ControlFlag.toString()).attr("ControlFlag"))
            if (HmiSpecVersion != null) a.add(HmString(HmiSpecVersion).attr("HmiSpecVersion"))
            return Instance("com.sap.aii.util.hmi.core.msg.HmiRequest", a)
        }

        fun encodeToString() = hmserializer.encodeToString(this.instance())

    }

    data class HmiResponse(
        val ClientId: String,
        val RequestId: String,
        val MethodOutput: HmiMethodOutput? = null,
        val MethodFault: HmiMethodFault? = null,
        val CoreException: HmiCoreException? = null,
        val ControlFlag: Int = 0,
        val HmiSpecVersion: String? = null
    ) {
        companion object {
            fun from(i: Instance): HmiResponse {
                require(i.typeid == "com.sap.aii.utilxi.hmi.core.msg.HmiResponse")
                val clientId = i.string("ClientId")
                val requestId = i.string("RequestId")
                val cf = i.string("ControlFlag")!!.toInt()
                val hv = i.string("HmiSpecVersion")
                val i2 = i.instance("MethodOutput")
                val hmo = HmiMethodOutput.from(i2)
                val hmf = HmiMethodFault.from(i.instance("MethodFault"))
                val hce = HmiCoreException.from(i.instance("CoreException"))
                return HmiResponse(clientId!!, requestId!!, hmo, hmf, hce, cf, hv)
            }
        }
    }

}
