package karlutka.parsers.pi

import nl.adaptivity.xmlutil.*
import java.io.*

/**
 * Базовый класс работы с HMI. Помимо Instance, Attribute и Value здесь лишь запрос и ответ. Можно добавить фолт.
 * После освоения XmlMixed в должной мере, переписать этот StAX-разбор на смешанный.
 *
 * Остальные использования HMI см.в HmiUsages и SimpleQuery
 */
class Hmi {
    class Instance(
        val typeid: String,
        val attributes: List<Attribute> = listOf(),
    ) {
        fun encodeToWriter(xw: XmlWriter) {
            xw.startTag(null, "instance", null)
            xw.attribute(null, "typeid", null, typeid)
            attributes.forEach { it.write(xw) }
            xw.endTag(null, "instance", null)
        }

        fun encodeToStream(os: OutputStream, declMode: XmlDeclMode = XmlDeclMode.None) {
            val xw = PlatformXmlWriter(os, "UTF-8", false, declMode)
            encodeToWriter(xw)
            xw.endDocument()
        }

        fun encodeToWriter(ow: Writer, declMode: XmlDeclMode = XmlDeclMode.None) {
            val xw = PlatformXmlWriter(ow, false, declMode)
            encodeToWriter(xw)
            xw.endDocument()
        }

        fun encodeToString(declMode: XmlDeclMode = XmlDeclMode.None) : String {
            val sw = StringWriter()
            encodeToWriter(sw, declMode)
            sw.close()
            return sw.toString()
        }

        fun toHmiRequest(): HmiRequest {
            require(typeid.endsWith("HmiRequest"))
            val ats = attributes.filter { it.leave_typeid == "string" }.associate { Pair(it.name, it.value[0].text) }
            val level = HmiUsages.ApplCompLevel()    // для разбора запроса нам пофиг какой уровень
            val input = attributes.find { it.name.endsWith("MethodInput") }
            val inpm: Map<String, String?>?
            val inpType: String?
            if (input != null) {
                inpm = mutableMapOf()
                inpType = input.value[0].instance!!.typeid
                val a = input.value[0].instance!!.attributes[0]
                require(a.name == "Parameters")
                a.value.forEach { v ->
                    require(v.instance!!.attributes.size == 2)
                    val a1 = v.instance.attributes[0]
                    val a2 = v.instance.attributes[1]
                    require(a1.name == "Key" && a2.name == "Value")
                    inpm[a1.value[0].text!!] = a2.value[0].text
                }
            } else {
                inpm = null
                inpType = null
            }

            // только обязательные поля
            return HmiRequest(
                typeid, ats["ClientId"]!!, ats["RequestId"]!!, level, inpType, inpm, ats["MethodId"]!!, ats["ServiceId"]!!
            )
        }
    }

    class Attribute(
        val isleave: Boolean = true,
        val leave_typeid: String? = null,
        val name: String,
        val value: MutableList<Value> = mutableListOf(),
    ) {
        constructor(name: String, value: String?) : this(true, "string", name, mutableListOf(Value(0, false, null, value)))
        constructor(name: String) : this(false, null, name, mutableListOf(Value(0, false, null, null)))
        constructor(name: String, inst: Instance) : this(false, null, name, mutableListOf(Value(0, false, inst, null)))
        constructor(name: String, value: Value) : this(false, null, name, mutableListOf(value))

        fun write(xw: XmlWriter) {
            xw.startTag(null, "attribute", null)
            xw.attribute(null, "isleave", null, isleave.toString())
            if (leave_typeid != null) xw.attribute(null, "leave_typeid", null, leave_typeid)
            xw.attribute(null, "name", null, name)
            value.forEach { it.write(xw) }
            xw.endTag(null, "attribute", null)
        }
    }

    class Value(
        val index: Int = 0, val isnull: Boolean = false,
        // Хранит или instance или string. При наличии instance, строку не смотрим
        val instance: Instance? = null, val text: String? = null,
    ) {
        fun write(xw: XmlWriter) {
            xw.startTag(null, "value", null)
            xw.attribute(null, "index", null, index.toString())
            xw.attribute(null, "isnull", null, isnull.toString())
            if (instance != null) {
                instance.encodeToWriter(xw)
            } else if (text != null) {
                xw.text(text)
            }
            xw.endTag(null, "value", null)
        }
    }

    class HmiRequest(
        val typeid: String = typeIdAiiHmiRequest,
        val ClientId: String,
        val RequestId: String,
        val ClientLevel: HmiUsages.ApplCompLevel,
        val methodInputTypeId: String?,
        val MethodInput: Map<String, String?>?,     // что если не все параметры строковые?
        val MethodId: String,
        val ServiceId: String,
        val ClientUser: String? = null,
        val ClientPassword: String? = null,
        val ClientLanguage: String = "EN",
        val RequiresSession: Boolean = true,
        val ServerLogicalSystemName: String? = null,
        val ServerApplicationId: String? = null,
        val HmiSpecVersion: String = "1.0",
        val ControlFlag: String = "0",
    ) {
        constructor(inst: Instance) : this(
            inst.typeid,
            inst.attributes.find { it.name == "ClientId" }!!.value[0].text!!,
            inst.attributes.find { it.name == "RequestId" }!!.value[0].text!!,
            HmiUsages.ApplCompLevel(inst.attributes.find { it.name == "ClientLevel" }!!.value[0].instance!!),
            inst.attributes.find { it.name == "MethodInput" }!!.value[0].instance!!.typeid,
            // единственный случай что нашёл это Parameters = inst.attributes.find { it.name == "MethodInput" }!!.value[0].instance?.attributes?.get(0)?
            inst.attributes.find { it.name == "MethodInput" }!!.value[0].instance?.attributes?.get(0)?.value?.associate { v ->
                Pair(
                    v.instance?.attributes?.find { it.name == "Key" }?.value?.get(0)?.text!!,
                    v.instance.attributes.find { it.name == "Value" }?.value?.get(0)?.text!!
                )
            } ?: mapOf(),
            inst.attributes.find { it.name == "MethodId" }!!.value[0].text!!,
            inst.attributes.find { it.name == "ServiceId" }!!.value[0].text!!,
            inst.attributes.find { it.name == "ClientUser" }?.value?.get(0)?.text,
            inst.attributes.find { it.name == "ClientPassword" }?.value?.get(0)?.text,
            inst.attributes.find { it.name == "ClientLanguage" }?.value?.get(0)?.text ?: "EN",
            inst.attributes.find { it.name == "RequiresSession" }?.value?.get(0)?.text?.toBooleanStrict() ?: false,
            inst.attributes.find { it.name == "ServerLogicalSystemName" }?.value?.get(0)?.text,
            inst.attributes.find { it.name == "ServerApplicationId" }?.value?.get(0)?.text,
            inst.attributes.find { it.name == "HmiSpecVersion" }?.value?.get(0)?.text ?: "1.0",
            inst.attributes.find { it.name == "ControlFlag" }?.value?.get(0)?.text ?: "0",
        )

        fun toInstance(): Instance {
            val lst = mutableListOf<Attribute>()
            lst.add(Attribute("ClientId", ClientId))
            lst.add(Attribute("ClientLanguage", ClientLanguage))
            lst.add(Attribute("ClientLevel", ClientLevel.toInstance()))
            lst.add(Attribute("ClientPassword", ClientPassword))
            lst.add(Attribute("ClientUser", ClientUser))
            lst.add(Attribute("ControlFlag", ControlFlag))
            lst.add(Attribute("HmiSpecVersion", HmiSpecVersion))
            lst.add(Attribute("MethodId", MethodId))
            if (MethodInput != null) {
                val lz = MethodInput.map {
                    Instance(typeIdEntryStringString, listOf(Attribute("Key", it.key), Attribute("Value", it.value)))
                }.mapIndexed { ix, inst -> Value(ix, false, inst) }.toMutableList()
                val inst = Instance(typeIdAiiHmiInput, listOf(Attribute(false, null, "Parameters", lz)))
                lst.add(Attribute("MethodInput", inst))
            } else {
                lst.add(Attribute("MethodInput", Value(0, true)))
            }
            lst.add(Attribute("RequestId", RequestId))
            lst.add(Attribute("RequiresSession", RequiresSession.toString()))
            lst.add(Attribute("ServerApplicationId", ServerApplicationId))          //возможно только для ESR
            lst.add(Attribute("ServerLogicalSystemName", ServerLogicalSystemName))  //возможно только для ESR
            lst.add(Attribute("ServiceId", ServiceId))
            return Instance(typeid, lst)
        }

        fun copyToResponse(contentType: String, Return: String): HmiResponse {
            return HmiResponse(
                typeid.replace("HmiRequest", "HmiResponse"),
                ClientId,
                RequestId,
                methodInputTypeId!!.replace("HmiMethodInput", "HmiMethodOutput"),
                contentType,
                Return,
                HmiSpecVersion,
                ControlFlag
            )
        }
    }

    data class HmiResponse(
        val typeid: String,
        val ClientId: String,
        val RequestId: String,
        val methodOutputTypeId: String?,
        val MethodOutputContentType: String?,
        val MethodOutputReturn: String?,
        val HmiSpecVersion: String,
        val ControlFlag: String,
        val MethodFault: Instance? = null,
        val CoreException: Instance? = null,
    ) {
        constructor(inst: Instance) : this(
            inst.typeid,
            inst.attributes.find { it.name == "ClientId" }!!.value[0].text!!,
            inst.attributes.find { it.name == "RequestId" }!!.value[0].text!!,
            inst.attributes.find { it.name == "MethodOutput" }?.value?.get(0)?.instance?.typeid,
            inst.attributes.find { it.name == "MethodOutput" }?.value?.get(0)?.instance?.attributes?.find { it.name == "ContentType" }?.value?.get(0)?.text,
            inst.attributes.find { it.name == "MethodOutput" }?.value?.get(0)?.instance?.attributes?.find { it.name == "Return" }?.value?.get(0)?.text,
            inst.attributes.find { it.name == "HmiSpecVersion" }!!.value[0].text!!,
            inst.attributes.find { it.name == "ControlFlag" }!!.value[0].text!!,
            inst.attributes.find { it.name == "MethodFault" }?.value?.get(0)?.instance,
            inst.attributes.find { it.name == "CoreException" }?.value?.get(0)?.instance
        )

        fun toInstance(): Instance {
            val lst = mutableListOf<Attribute>()
            lst.add(Attribute("ClientId", ClientId))
            lst.add(Attribute("ControlFlag", ControlFlag))
            if (CoreException == null)
                lst.add(Attribute("CoreException", Value(0, true)))
            else
                lst.add(Attribute("CoreException", CoreException))
            lst.add(Attribute("HmiSpecVersion", HmiSpecVersion))
            if (MethodFault == null)
                lst.add(Attribute("MethodFault", Value(0, true)))
            else
                lst.add(Attribute("MethodFault", MethodFault))
            if (methodOutputTypeId != null) {
                val mo = Instance(
                    methodOutputTypeId, listOf(
                        Attribute("ContentType", MethodOutputContentType),
                        Attribute("Return", MethodOutputReturn),
                    )
                )
                lst.add(Attribute("MethodOutput", mo))
            } else {
                lst.add(Attribute("MethodOutput", Value(0, true)))
            }

            lst.add(Attribute("RequestId", RequestId))

            return Instance(typeid, lst)
        }
    }

    companion object {
        const val typeIdAiiHmiRequest = "com.sap.aii.util.hmi.core.msg.HmiRequest"
        const val typeIdAiiHmiInput = "com.sap.aii.util.hmi.api.HmiMethodInput"
        const val typeIdAiiApplCompLevel = "com.sap.aii.util.applcomp.ApplCompLevel"
        const val typeIdEntryStringString = "com.sap.aii.util.hmi.core.gdi2.EntryStringString"

        fun decodeInstanceFromReader(xr: XmlReader): Instance {
            xr.skipPreamble()
            val ac = xr.attributeCount
            require(ac == 1)
            val a = xr.attributes[0]
            require(a.localName == "typeid")
            val lst = decodeAttributes(xr)
            return Instance(a.value, lst)
        }

        private fun decodeAttributes(xr: XmlReader): List<Attribute> {
            val lst = mutableListOf<Attribute>()
            var li: Instance? = null            // latest instance
            var la: Attribute? = null           // latest attribute
            var lv: Value? = null               // latest value
            val lt = mutableListOf<String>()

            while (xr.hasNext()) {
                xr.next()
                when {
                    xr.isStartElement() && xr.localName == "attribute" -> {
                        val ats = xr.attributes.associate { Pair(it.localName, it.value) }
                        la = Attribute(ats["isleave"]!!.toBooleanStrict(), ats["leave_typeid"], ats["name"]!!)
                        lst.add(la)
                    }
                    xr.isStartElement() && xr.localName == "value" -> {
                        val ats = xr.attributes.associate { Pair(it.localName, it.value) }
                        lv = Value(ats["index"]!!.toInt(), ats["isnull"]!!.toBooleanStrict())
                    }
                    xr.isStartElement() && xr.localName == "instance" && lv != null -> {
                        li = decodeInstanceFromReader(xr)
                    }
                    xr.isCharacters() && lv != null -> {
                        lt.add(xr.text)
                    }
                    xr.isEndElement() && xr.localName == "attribute" -> {
                        la = null
                    }
                    xr.isEndElement() && xr.localName == "value" -> {
                        if (li != null) lt.clear() //тексты только если нет instance, иначе всякие \n лезут
                        la!!.value.add(Value(lv!!.index, lv.isnull, li, lt.joinToString("")))
                        lv = null
                        lt.clear()
                        li = null
                    }
                    xr.isEndElement() && xr.localName == "instance" -> {
                        return lst
                    }
                }
            }
            require(false) { "Possible incorrect XML or wrong HMI structure" }
            return listOf()
        }

        fun decodeInstanceFromString(s: String): Instance {
            return decodeInstanceFromReader(PlatformXmlReader(StringReader(s)))
        }
    }
}
