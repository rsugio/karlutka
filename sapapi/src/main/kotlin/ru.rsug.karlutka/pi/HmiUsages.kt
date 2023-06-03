package ru.rsug.karlutka.pi

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
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
        fun applCompLevel(): ApplCompLevel = ApplCompLevel(release, SP)
    }

    class ApplCompLevel(
        val Release: String = "7.5", val SupportPackage: String = "*",
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

        companion object {
            fun decodeFromString(sxml: String): TestExecutionRequest {
                TODO()
                //return Hm.hmserializer.decodeFromString(sxml)
            }

            fun create(
                swcv: PCommon.VC, typeId: MPI.ETypeID = MPI.ETypeID.MAPPING, //для ММ - XI_TRAFO
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
                val ref = Ref(swcv, PCommon.Key(typeId, "00000000000000000000000000000000", listOf(name, namespace)))
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