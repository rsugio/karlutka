package karlutka.parsers.pi

import karlutka.util.KtorClient         //TODO убрать нафиг
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

class HmiUsages {
    @Serializable
    @XmlSerialName("Services", "", "")
    // Читается по /rep/getregisteredhmimethods/int?container=any, для /dir надо искать или составлять вручную
    class HmiServices(
        val list: List<HmiService> = listOf()
    ) {
        constructor(r: Hmi.HmiResponse): this(XML.decodeFromString<HmiServices>(r.MethodOutputReturn!!).list)
    }

    @Serializable
    @XmlSerialName("service", "", "")
    class HmiService(
        val serviceid: String,  //mappingtestservice
        val methodid: String,   //executemappingmethod
        val release: String,    //7.31
        val SP: String,         //0 или *
        @Serializable val subrelease: String, //*
        val patchlevel: String, //*
    ) {
        var url: String = ""
        fun applCompLevel(): ApplCompLevel = ApplCompLevel(release, SP)
//        fun url() = "/rep/$serviceid/int?container=any"
    }

    class ApplCompLevel(
        val Release: String = "7.0", val SupportPackage: String = "*",
    ) {
        constructor(s: HmiService) : this(s.release, s.SP)
        constructor(inst: Hmi.Instance) : this(
            inst.attributes.find { it.name == "Release" }?.value?.get(0)?.text ?: "7.0",
            inst.attributes.find { it.name == "SupportPackage" }?.value?.get(0)?.text ?: "0",
        )

        fun toInstance(): Hmi.Instance {
            return Hmi.Instance(
                Hmi.typeIdAiiApplCompLevel,
                listOf(Hmi.Attribute("Release", Release), Hmi.Attribute("SupportPackage", SupportPackage))
            )
        }
    }

    class HmiMethodInput(val input: Map<String, String?>) {
        constructor(key: String, value: String?) : this(mapOf(key to value))

        fun attr(name: String = "MethodInput"): Hmi.Attribute? {
            // пример на несколько - см /test/resources/pi_HMI/03many.xml
            return null
//            val lst = mutableListOf<Value>()
//            var ix = 0
//            input.map { e ->
//                val inst = Instance(
//                    //TODO тоже ошибка - хардкод имени класса
//                    "com.sap.aii.util.hmi.core.gdi2.EntryStringString", listOf(
//                        HmString(e.key).attr("Key"), HmString(e.value).attr("Value")
//                    )
//                )
//                lst.add(Value(ix++, false, listOf(inst)))
//
//            }
//            val params = Attribute(false, null, "Parameters", lst)
//            //TODO здесь неправильно хардкодить имя класса, оно может слегка меняться в разных модулях
//            val inst = Instance("com.sap.aii.util.hmi.api.HmiMethodInput", listOf(params))
//            return Attribute(
//                false, null, name, listOf(
//                    Value(0, false, listOf(inst))
//                )
//            )
        }
    }

    class HmiMethodOutput(val ContentType: String, val Return: String) {
        companion object {
            fun from(i: Hmi.Instance?): HmiMethodOutput? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodOutput")
                return null // HmiMethodOutput(i.string("ContentType")!!, i.string("Return")!!)
            }
        }
    }

    @Suppress("unused")
    class HmiMethodFault(
        val LocalizedMessage: String,
        val Severity: String,
        val OriginalStackTrace: String?,
        val RootCauseAsString: String?,
        val Details: String?,
    ) {
        companion object {
            fun from(i: Hmi.Instance?): HmiMethodFault? {
                return null
//                if (i == null) return null
//                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodFault")
//                return HmiMethodFault(
//                    i.string("LocalizedMessage")!!,
//                    i.string("Severity")!!,
//                    i.string("OriginalStackTrace"),
//                    i.string("RootCauseAsString"),
//                    i.string("Details")
//                )
            }
        }
    }

    @Suppress("unused")
    class HmiCoreException(
        val LocalizedMessage: String,
        val Severity: String,
        val OriginalStackTrace: String?,
        val SubtypeId: String?,
    ) {
        companion object {
            fun from(i: Hmi.Instance?): HmiCoreException? {
                return null
//                if (i == null) return null
//                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiCoreException")
//                return HmiCoreException(
//                    i.string("LocalizedMessage")!!, i.string("Severity")!!, i.string("OriginalStackTrace"), i.string("SubtypeId")
//                )
            }
        }
    }

    class HmiRequest(
        val ClientId: String,               // IGUID
        val RequestId: String,              // IGUID
        val ClientLevel: ApplCompLevel,     // UriElement
        val HmiMethodInput: HmiMethodInput, // UriElement
        val MethodId: String,               // HmiMethodInput
        val ServiceId: String,
        val ClientUser: String = "",
        val ClientPassword: String = "",
        val ClientLanguage: String = "EN",
        val RequiresSession: Boolean = true,
        val ServerLogicalSystemName: String? = null,
        val ServerApplicationId: String? = null,
        val HmiSpecVersion: String = "1.0",
        val ControlFlag: Int = 0,
    ) {
        fun instance(): Hmi.Instance? {
            return null
//            val a = mutableListOf<Attribute>()
//            a.add(HmString(ClientId).attr("ClientId"))
//            a.add(HmString(ClientLanguage).attr("ClientLanguage"))
//            //a.add(ClientLevel.attr())
//            a.add(HmString(ClientUser).attr("ClientUser"))
//            a.add(HmString(ClientPassword).attr("ClientPassword"))
//            a.add(HmString(ControlFlag.toString()).attr("ControlFlag"))
//            a.add(HmString(HmiSpecVersion).attr("HmiSpecVersion"))
//            a.add(HmString(MethodId).attr("MethodId"))
//            //a.add(HmiMethodInput.attr())
//
//            a.add(HmString(RequestId).attr("RequestId"))
//            a.add(HmString(RequiresSession.toString()).attr("RequiresSession"))
//            a.add(HmString(ServerLogicalSystemName).attr("ServerLogicalSystemName"))
//            a.add(HmString(ServerApplicationId).attr("ServerApplicationId"))
//            a.add(HmString(ServiceId).attr("ServiceId"))
//            return Instance("com.sap.aii.util.hmi.core.msg.HmiRequest", a)
        }

        fun encodeToString(): String = TODO() //Hm.hmserializer.encodeToString(this.instance())
    }

    class HmiResponse(
        val ClientId: String? = null,
        val RequestId: String? = null,
        val MethodOutput: HmiMethodOutput? = null,
        val MethodFault: HmiMethodFault? = null,
        val CoreException: HmiCoreException? = null,
        val ControlFlag: Int = 0,
        val HmiSpecVersion: String? = null,
    ) {
        fun toQueryResult(task: KtorClient.Task): SimpleQuery.Result {
            TODO()
//broken            requireNotNull(MethodOutput) { "Нет данных в запросе $RequestId задача ${task.path} remark=${task.remark}" }
//            try {
//                val v = Hm.hmserializer.decodeFromString<HmUsages.QueryResult>(MethodOutput.Return)
//                return v
//            } catch (e: UnknownXmlFieldException) {
//                System.err.println("Ошибка разбора запроса $RequestId задача ${task.path} remark=${task.remark}")
//                throw e
//            }
        }

        companion object {
            fun from(i: Hmi.Instance): HmiResponse? {
                return null
//                require(i.typeid == "com.sap.aii.utilxi.hmi.core.msg.HmiResponse")
//                val clientId = i.string("ClientId")
//                val requestId = i.string("RequestId")
//                val cf = i.string("ControlFlag")!!.toInt()
//                val hv = i.string("HmiSpecVersion")
//                val hmo = HmiMethodOutput.from(i.attribute("MethodOutput").instance)
//                val hmf = HmiMethodFault.from(i.attribute("MethodFault").instance)
//                val hce = HmiCoreException.from(i.attribute("CoreException").instance)
//                return HmiResponse(clientId, requestId, hmo, hmf, hce, cf, hv)
            }

            fun parse(task: KtorClient.Task): HmiResponse {
                //return from(hmserializer.decodeFromReader(task.bodyAsXmlReader()))
                return HmiResponse()
            }
        }
    }


    // Прикладные сервисы поверх HMI

    @Serializable
    @XmlSerialName("ref", "", "")
    class Ref(
        @XmlElement(true) @XmlSerialName("vc", "", "") val vc: PCommon.VC,
        @XmlSerialName("key", "", "") val key: PCommon.Key,
        @XmlElement(true) val vspec: VSpec? = null,
    ) {
        @Serializable
        @XmlSerialName("vspec", "", "")
        // Версия объекта
        class VSpec(
            @Serializable val type: Int,        // 4 для CC. Или это номер версии?
            @Serializable val id: String,       // versionid
            @Serializable val deleted: Boolean, // признак удаления
        )

        override fun toString() = "Ref($key)"
    }

    @Serializable
    @XmlSerialName("testExecutionRequest", "", "")
    class TestExecutionRequest(
        @XmlElement(true) val ref: Ref,
        @XmlElement(true) val testData: TestData,
    ) {

        @Serializable
        @XmlSerialName("testData", "", "")
        class TestData(
            @XmlElement(true) val inputXml: String,
            @XmlElement(true) val parameters: Parameters,
            @XmlElement(true) val testParameters: TestParameters? = null,
            @XmlElement(true) val traceLevel: Int = 3,
        )

        @Serializable
        @XmlSerialName("parameters", "", "")
        class Parameters(
            @XmlElement(true) val testParameterInfo: TestParameterInfo,
        )

        @Serializable
        @XmlSerialName("testParameters", "", "")
        class TestParameters(
            @XmlElement(true) val direction: String,
            @XmlElement(true) val fromStep: Int,
            @XmlElement(true) val toStep: Int,
        )

        @Serializable
        @XmlSerialName("testParameterInfo", "", "")
        class TestParameterInfo(
            @XmlElement(true) @XmlSerialName("HeaderParameters", "", "") val HeaderParameters: HIParameters,
            @XmlElement(true) @XmlSerialName("ImportingParameters", "", "") val ImportingParameters: HIParameters,
        )

        @Serializable
        class HIParameters(
            @XmlElement(true) val properties: Properties,
        )

        @Serializable
        @XmlSerialName("properties", "", "")
        class Properties(
            @XmlElement(true) val property: List<Property> = listOf(),
        )

        @Serializable
        @XmlSerialName("property", "", "")
        class Property(
            val name: String,
            @XmlValue(true) val value: String = "",
        )

        fun encodeToString(): String = TODO() // Hm.hmserializer.encodeToString(this)

        companion object {
            fun decodeFromString(sxml: String): TestExecutionRequest {
                TODO()
                //return Hm.hmserializer.decodeFromString(sxml)
            }

            fun create(
                swcv: PCommon.VC, typeId: String = "MAPPING", //для ММ - XI_TRAFO
                name: String, namespace: String, testXml: String,
            ): TestExecutionRequest {
                val params = listOf(
                    Property("TimeSent", ""),
                    Property("SenderSystem", ""),
                    Property("SenderParty", ""),
                    Property("InterfaceNamespace", ""),
                    Property("Interface", ""),
                    Property("SenderPartyScheme", ""),
                    Property("ReceiverPartyAgency", ""),
                    Property("RefToMessageId", ""),
                    Property("ReceiverPartyScheme", ""),
                    Property("SenderName", ""),
                    Property("Profiling", ""),
                    Property("MessageId", ""),
                    Property("VersionMajor", ""),
                    Property("ReceiverName", ""),
                    Property("ReceiverParty", ""),
                    Property("ProcessingMode", ""),
                    Property("SenderService", ""),
                    Property("ReceiverNamespace", ""),
                    Property("ConversationId", ""),
                    Property("VersionMinor", ""),
                    Property("SenderNamespace", ""),
                    Property("ReceiverService", ""),
                    Property("ReceiverSystem", ""),
                    Property("SenderPartyAgency", ""),
                )
                // здесь oid ничем не помогает
                val ref = Ref(swcv, PCommon.Key(typeId, null, listOf(name, namespace)))
                val td = TestData(
                    testXml, Parameters(
                        TestParameterInfo(
                            HIParameters(Properties(params)), HIParameters(Properties(listOf()))
                        )
                    ), TestParameters("REQUEST", 0, 0), 3
                )
                return TestExecutionRequest(ref, td)
            }
        }
    }

    @Serializable
    @XmlSerialName("testExecutionResponse", "", "")
    class TestExecutionResponse(
        @XmlElement(true) val outputXML: String? = null,
        @XmlElement(true) val exportParameters: String? = null,       //TODO неизвестный тип, подобрать
        @XmlElement(true) val messages: Messages? = null,
        @XmlElement(true) val exception: TestException? = null,
        @XmlElement(true) val stacktrace: String? = null,
    ) {
        companion object {
            fun decodeFromString(sxml: String): TestExecutionResponse {
                TODO() //return Hm.hmserializer.decodeFromString(sxml)
            }
        }
    }

    @Serializable
    @XmlSerialName("exception", "", "")
    class TestException(
        @XmlElement(true) val type: String,
        @XmlElement(true) @Contextual @XmlSerialName("message", "", "") val message: CompactFragment,
    )

    @Serializable
    @XmlSerialName("message", "", "")
    class TestMessage(
        val level: String? = "INFO", @Serializable @XmlValue(true) val text2: String? = null,
    )

    @Serializable
    @XmlSerialName("messages", "", "")
    class Messages(
        @XmlElement(true) val message: List<TestMessage>,
    )

    @Serializable
    @XmlSerialName("configuration", "", "")
    class DirConfiguration(
        @XmlElement(true) val user: User,
        @XmlElement(true) val repository: Repository,
        @XmlElement(true) val properties: Properties,
        @XmlElement(true) val FEATURES: Features,
        @XmlElement(true) val Roles: _Roles,
        @XmlElement(true) val AdapterEngines: _AdapterEngines,
        @XmlElement(true) val CacheInstances: _CacheInstances,
    ) {
        @Serializable
        @XmlSerialName("user", "", "")
        class User(
            @XmlElement(true) val userid: String,
        )

        @Serializable
        @XmlSerialName("repository", "", "")
        class Repository(
            @XmlElement(true) val type: String,
            @XmlElement(true) val host: String,
            @XmlElement(true) val httpport: String, // могут быть 50000 или "@com.sap.aii.server.httpsport.repository@"
            @XmlElement(true) val httpsport: String,
        )

        @Serializable
        @XmlSerialName("properties", "", "")
        class Properties(
            @XmlElement(true) val property: List<Property>,
        )

        @Serializable
        @XmlSerialName("property", "", "")
        class Property(
            val key: String,
            val value: String,
        )

        @Serializable
        @XmlSerialName("FEATURES", "", "")
        class Features(
            @XmlElement(true) val FEATURE: List<Feature>,
        )

        @Serializable
        @XmlSerialName("FEATURE", "", "")
        class Feature(
            @Serializable val FEATUREID: String,
        )

        @Serializable
        @XmlSerialName("Roles", "", "")
        class _Roles(
            @XmlElement(true) val Role: List<Role>,
        )

        @Serializable
        @XmlSerialName("Role", "", "")
        class Role(
            @Serializable val RoleID: String,
        )

        @Serializable
        @XmlSerialName("AdapterEngines", "", "")
        class _AdapterEngines(
            @XmlElement(true) val AdapterFrameWork: List<AdapterFrameWork>,
        )

        @Serializable
        @XmlSerialName("AdapterFrameWork", "", "")
        class AdapterFrameWork(
            @XmlElement(true) val key: String,
            @XmlElement(true) val name: String,
            @XmlElement(true) val isCentral: Boolean,
            @XmlElement(true) val httpUrl: String,
            @XmlElement(true) val httpsUrl: String,
        )

        @Serializable
        @XmlSerialName("CacheInstances", "", "")
        class _CacheInstances(
            @XmlElement(true) val CacheInstance: List<CacheInstance>,
        )

        @Serializable
        @XmlSerialName("CacheInstance", "", "")
        class CacheInstance(
            @XmlElement(true) val name: String,
            @XmlElement(true) val mode: String,
            @XmlElement(true) val displayName: String,
        )

        companion object {
            fun decodeFromString(sxml: String): DirConfiguration = TODO() //Hm.hmserializer.decodeFromString<DirConfiguration>(sxml)
//            fun decodeFromXmlReader(xmlReader: XmlReader) = hmserializer.decodeFromReader<DirConfiguration>(xmlReader)
        }
    }

    @Serializable
    @XmlSerialName("list", "", "")
    class ReadListRequest(
        @XmlElement(true) val type: Type,
    ) {
        fun encodeToString(): String = TODO() // Hm.hmserializer.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("type", "", "")
    class Type(
        val id: String,
        @XmlElement(true) val ref: Ref,

        @Serializable val ADD_IFR_PROPERTIES: Boolean = true,
        @Serializable val STOP_ON_FIRST_ERROR: Boolean = true,
        @Serializable val RELEASE: String = "7.0",
        @Serializable val DOCU_LANG: String = "EN",
        @Serializable val XSD_VERSION: String = "http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/",
        @Serializable val WITH_UI_TEXTS: Boolean = false,
        @Serializable val ADD_ENHANCEMENTS: Boolean = false,
        @Serializable val WSDL_XSD_GEN_MODE: String = "EXTERNAL",
    )

// общие: DOCU_LANG, RELEASE, DOCU_LANG
// namespdecl: ADD_IFR_PROPERTIES=true, STOP_ON_FIRST_ERROR = true|false
// ifmtypedef: XSD_VERSION="http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/"
//          WITH_UI_TEXTS="false" ADD_ENHANCEMENTS="false" WSDL_XSD_GEN_MODE="EXTERNAL"
// XI_TRAFO:
// MAPPING:

    companion object {
        fun decodeHmiServicesFromReader(r: XmlReader) = XML.Companion.decodeFromReader<HmiServices>(r)
        fun decodeHmiServicesFromResponse(r: Hmi.HmiResponse) = XML.Companion.decodeFromString<HmiServices>(r.MethodOutputReturn!!)
    }
}