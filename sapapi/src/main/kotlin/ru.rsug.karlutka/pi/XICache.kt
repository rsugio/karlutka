package ru.rsug.karlutka.pi

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Suppress("unused")
class XICache {
    enum class EOp { EQ, NE, CP, EX }

    @Serializable
    @XmlSerialName("CacheRefresh", "", "")
    class CacheRefresh(
        val DELETED_OBJECTS: DELETED_OBJECTS? = null,
        val Party: List<Party>,
        val Channel: List<Channel>,
        val AllInOne: List<AllInOne>,
        val ServiceInterface: List<ServiceInterface>,
        val MPP_MAP: List<MPP_MAP>,
        @XmlSerialName("AdapterMetaData", "", "") val AdapterMetaData: List<@Contextual CompactFragment>,
        @XmlSerialName("AlertRule", "", "") val AlertRule: List<@Contextual CompactFragment>,
        @XmlSerialName("SWCV", "", "") val SWCV: List<@Contextual CompactFragment>,
        @XmlSerialName("Service", "", "") val Service: List<@Contextual CompactFragment>,
        @XmlSerialName("SXI_CONT", "", "") val SXI_CONT: @Contextual CompactFragment? = null,  // контейнер параметров, не нужен
        @XmlSerialName("SXI_PROP", "", "") val SXI_PROP: List<@Contextual CompactFragment>,
    ) {
        fun isEmpty() = DELETED_OBJECTS == null && Party.isEmpty() && Channel.isEmpty() && AllInOne.isEmpty()
                && ServiceInterface.isEmpty() && MPP_MAP.isEmpty() && AdapterMetaData.isEmpty()
                && Service.isEmpty() && SXI_CONT == null
    }

    @Serializable
    @XmlSerialName("DELETED_OBJECTS", "", "")
    class DELETED_OBJECTS(
        @XmlElement val SAPXI_OBJECT_KEY: List<SAPXI_OBJECT_KEY> = listOf(),
    )

    @Serializable
    @XmlSerialName("SAPXI_OBJECT_KEY", "", "")
    class SAPXI_OBJECT_KEY(
        @XmlElement val OBJECT_ID: String,
        @XmlElement @XmlSerialName("OBJECT_TYPE") val OBJECT_TYPE: MPI.ETypeID,
        @XmlElement val SP_NUMBER: Int?,        // только на репозитории
        @XmlElement val VERSION_ID: String?,     // только на репозитории
    )

    @Serializable
    @XmlSerialName("Party", "", "") //"""urn:sap-com:xi:xiParty", "cp")
    class Party(
        @XmlElement val PartyName: String,
        @XmlElement val PartyObjectId: String,
        @XmlElement val PartyIdentifier: List<PartyIdentifier>,
    )

    @Serializable
    @XmlSerialName("PartyIdentifier", "", "") // "urn:sap-com:xi:xiParty", "cp")
    class PartyIdentifier(
        @XmlElement val PartyIdentifierObjectId: String?,
        @XmlElement val Agency: String,
        @XmlElement val Schema: String,
        @XmlElement val Identifier: String,
    )

    @Serializable
    @XmlSerialName("Channel", "", "") // "urn:sap-com:xi:xiChannel", "cp")
    class Channel(
        @XmlElement val PartyName: String,
        @XmlElement val ServiceName: String,
        @XmlElement val ChannelName: String,
        @XmlElement val ChannelObjectId: String,
        @XmlElement val AdapterName: String,
        @XmlElement val AdapterNamespace: String,
        @XmlElement val AdapterSWCV: String,
        @XmlElement val EngineType: String,
        @XmlElement val EngineName: String,
        @XmlElement val ChannelDirection: String,
        @XmlElement val MessageProtocol: String,
        @XmlElement val MessageProtocolVersion: String,
        @XmlElement val TransportProtocol: String,
        @XmlElement val TransportProtocolVersion: String,
        @XmlElement val FromPartyAgency: String,
        @XmlElement val FromPartySchema: String,
        @XmlElement val ToPartyAgency: String,
        @XmlElement val ToPartySchema: String,
        @XmlElement val ChannelAttributes: ChannelAttributes,
        @XmlElement val PipelineAttributes: PipelineAttributes,
    ) {
        fun encodeToString() = parser.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("ChannelAttributes", "", "")// "urn:sap-com:xi:xiChannel", "cp")
    class ChannelAttributes(
        @XmlElement val AdapterTypeData: AdapterTypeData,
    )

    @Serializable
    @XmlSerialName("AdapterTypeData", "", "")
    class AdapterTypeData(
        @XmlElement val Attribute: List<Attribute>,
    )

    @Serializable
    @XmlSerialName("Attribute", "", "")
    class Attribute(
        @XmlElement val Name: String,
        @XmlElement val Namespace: String,
        @XmlElement val Value: Value,
    ) {
        fun valueAsString(): String {
            require(Value.isTable == null || Value.isTable == false)
            require(Value.isPassword == null || Value.isPassword == false)
            return if (Value.data.isEmpty())
                ""
            else if (Value.data.size == 1)
                Value.data[0].toString()
            else
                throw NotImplementedError()
        }
    }

    @Serializable
    @XmlSerialName("Value", "", "")
    class Value(
        @XmlElement(false) val isTable: Boolean?,
        @XmlElement(false) val isPassword: Boolean?,
        @XmlElement(false) val encryption: String?,
        @XmlValue val data: List<@Polymorphic Any?>,
    ) {
        companion object {
            fun module(): SerializersModule {
                return SerializersModule {
                    polymorphic(Any::class, String::class, String.serializer())
                    polymorphic(Any::class, Row::class, Row.serializer())
                }
            }
        }
    }

    @Serializable
    @XmlSerialName("Row", "", "")
    class Row(
        @XmlElement val field: List<RowField>,
    )

    @Serializable
    @XmlSerialName("Field", "", "")
    class RowField(
        @XmlElement val Fieldname: String,
        @XmlElement val Fieldvalue: String,
    )


    @Serializable
    @XmlSerialName("PipelineAttributes", "", "") // "urn:sap-com:xi:xiChannel", "cp")
    class PipelineAttributes(
        @XmlElement val PipelineData: PipelineData?,
        @XmlElement val ModuleData: ModuleData?,
    )

    @Serializable
    @XmlSerialName("PipelineData", "", "") // "urn:sap-com:xi:xiChannel", "cp")
    class PipelineData(
        @XmlElement val PipelineConfig: List<PipelineConfig>,
    )

    @Serializable
    @XmlSerialName("PipelineConfig", "", "") // "urn:sap-com:xi:xiChannel", "cp")
    class PipelineConfig(
        @XmlElement val PipelineConfigObjectId: String?,    //guid
        @XmlElement val Position: Int,
        @XmlElement val ModuleName: String,
        @XmlElement val ModuleType: Char,
        @XmlElement val ModuleNs: String,
    )

    @Serializable
    @XmlSerialName("ModuleData", "", "") // "urn:sap-com:xi:xiChannel", "cp")
    class ModuleData(
        val ModuleConfig: List<ModuleConfig>,
    )

    @Serializable
    @XmlSerialName("ModuleConfig", "", "") // "urn:sap-com:xi:xiChannel", "cp")
    class ModuleConfig(
        @XmlElement val ModuleConfigObjectId: String?,
        @XmlElement val ModuleNs: String,
        @XmlElement val ParamName: String,
        @XmlElement val ParamValue: String,
    )

    class AllInOneParsed(
        val routeId: String,
        val fromParty: String,
        val fromService: String,
        val toParty: String,
        val toService: String,
        val fromIface: String,
        val fromIfacens: String,
        val namespaceMapping: Map<String, String> = mapOf(),
        val ifNoReceiverFound: Int = 0,
        val defaultReceiver: Receiver? = null,
        val receivers: List<Receiver>,
        val condgroups: List<ConditionGroup>,
        val channelsR: List<ChannelR>,
        val senderCC: String,
        val senderAttr: List<Attribute>,
    ) {
        val key = "$fromParty|$fromService|{$fromIfacens}$fromIface|$toParty|$toService"
        var xmlDsl: String = ""

        class Receiver(
            val id: String,
            val party: String,
            val service: String,
        )

        class ChannelR(
            val id: String,
            val receiver: Receiver,
            val attrs: List<Attribute>,
        ) {
            val camel = attrs.filter { it.Namespace == "camel" }.associate { Pair(it.Name, it.valueAsString()) }
        }

        class ConditionGroup(
            val id: String,
            val receivers: List<Receiver> = listOf(),
            val condlinegroups: List<CondLineGroup> = listOf(),
        )

        class CondLineGroup(
            val compgroup: Int,
            val cond: List<CondLine> = listOf(),
        )

        class CondLine(
            val counter: Int,
            val xpath: String?,
            val cobj: String?,        //context object name, потребует своего xpath и отдельного чтения из ESR
            val cons: String?,        //context object namespace
            val op: EOp,
            val right: String,
        ) {
            constructor(c: CONDLINE) : this(
                c.COUNTER,
                if (c.LEXTRACTOR.TRD_EXTRACTOR.TYPE == "XP") c.LEXTRACTOR.TRD_EXTRACTOR.VALUE else null,
                c.LEXTRACTOR.TRD_EXTRACTOR.COBJNAME,
                c.LEXTRACTOR.TRD_EXTRACTOR.COBJNS,
                c.COMPOP,
                c.REXTRACTOR.TRD_EXTRACTOR.VALUE //> и < экранируются сами
            )

            fun xpath() : String {
                val apos = right.contains('\'')
                val quot = right.contains('"')
                val r = when {
                    apos && quot -> {
                        // Экранирование требует разбирательств с xmlutil, так как иначе &apos; справедливо
                        // превращается в &amp;apos; а костылить суррогатные замены очень ненормально
                        // Возможно надо сделать DelegatingXmlWriter под выдачу маршрутов, заодно и неймспейсы
                        // приделать
                        TODO("both ' & \" are in right part of XPath-expression, unimplemented yet")
                    }
                    apos -> "\"$right\""
                    else -> "'$right'"
                }

                return when {
                    xpath != null && op == EOp.EQ -> "$xpath=$r"
                    xpath != null && op == EOp.NE -> "$xpath!=$r"
                    xpath != null && op == EOp.CP -> "$xpath~=$r"   //TODO
                    xpath != null && op == EOp.EX -> "boolean($xpath)"
                    else -> {
                        require(cobj != null)   //TODO
                        "false"
                    }
                }
            }
        }
    }

    @Serializable
    @XmlSerialName("AllInOne", "", "")
    class AllInOne(
        val version: String?,   // нет в полной
        @XmlElement val FromPartyName: String,
        @XmlElement val FromServiceName: String,
        @XmlElement val ToPartyName: String,
        @XmlElement val ToServiceName: String,
        @XmlElement val FromInterfaceName: String,
        @XmlElement val FromInterfaceNamespace: String,
        @XmlElement val AllInOneObjectId: String,
        @XmlElement val ParentURI: String?,             // нет в полной
        @XmlElement val SenderConnectivity: SenderConnectivity,
        @XmlElement val NamespaceMapping: NamespaceMapping?,        // нет в полной
        @XmlElement val NoReceiverBehaviour: NoReceiverBehaviour?,   // нет в полной
        @XmlElement val ReceiverConfigurations: ReceiverConfigurations,
        @XmlElement val ReceiverAssignmentList: ReceiverAssignmentList?,    // нет в полной
        @XmlElement val ReceiverConnectivityList: ReceiverConnectivityList,
        @XmlElement val Conditions: Conditions?,    // нет в полной
        @XmlElement val ScenarioConfiguration: ScenarioConfiguration?,      // нет в полной
    ) {
        fun encodeToString() = parser.encodeToString(this)

        /**
         * Для икохи возвращает перечень Channel.ObjectID
         */
        fun getChannelsOID(): List<String> {
            val lst = ReceiverConnectivityList.ReceiverConnectivity.map { it.ChannelObjectId }.distinct().toMutableList()
            lst.add(SenderConnectivity.ChannelObjectId)
            return lst
        }

        fun toParsed(lstChannels: List<Channel>): AllInOneParsed {
            requireNotNull(Conditions)
            requireNotNull(NamespaceMapping)
            requireNotNull(NoReceiverBehaviour)
            val receivers = ReceiverConfigurations.ReceiverConfiguration.map {
                requireNotNull(it.ReceiverId)    //TODO - для полного кэша есть пустые
                AllInOneParsed.Receiver(
                    it.ReceiverId,
                    it.Receiver.PartyExtractor.TRD_EXTRACTOR.VALUE, it.Receiver.ServiceExtractor.TRD_EXTRACTOR.VALUE
                )
            }
            val channels = ReceiverConnectivityList.ReceiverConnectivity.map { c ->
                AllInOneParsed.ChannelR(
                    c.ChannelObjectId,
                    receivers.find { r -> r.party == c.ToPartyName && r.service == c.ToServiceName }!!,
                    lstChannels.find { it.ChannelObjectId == c.ChannelObjectId }?.ChannelAttributes?.AdapterTypeData?.Attribute ?: listOf()
                )
            }.distinctBy { it.id }  //Если один канал несколько раз
            val conditions = Conditions.RDS_CONDSHORT.map { rds ->
                val ra = ReceiverAssignmentList!!.ReceiverAssignment.find { it.ReceiverConditionId == rds.CONDITIONID }?.ReceiverIds?.ReceiverId
                    ?: listOf()
                val gr = rds.CONDLINE.groupBy { it.COMPGROUP }.map { (COMPGROUP, lines) ->
                    AllInOneParsed.CondLineGroup(COMPGROUP, lines.map { AllInOneParsed.CondLine(it) })
                }
                AllInOneParsed.ConditionGroup(
                    rds.CONDITIONID,
                    ra.map { r -> receivers.find { it.id == r }!! },
                    gr
                )
            }
            assert(conditions.distinct().size == conditions.size)
            val sender = lstChannels.find { it.ChannelObjectId == SenderConnectivity.ChannelObjectId }!!
            return AllInOneParsed(
                sender.ChannelAttributes.AdapterTypeData.Attribute.find { it.Name == "routeId" && it.Namespace == "camel" }!!.valueAsString(),
                FromPartyName,
                FromServiceName,
                ToPartyName,
                ToServiceName,
                FromInterfaceName,
                FromInterfaceNamespace,
                NamespaceMapping.NSM.definition.associate { Pair(it.prefix, it.uri) },
                NoReceiverBehaviour.IfNoReceiverFound,
                receivers.find { it.id == NoReceiverBehaviour.DefaultReceiverId },
                receivers,
                conditions,
                channels,
                sender.ChannelName,
                sender.ChannelAttributes.AdapterTypeData.Attribute
            )
        }
    }

    @Serializable
    @XmlSerialName("SenderConnectivity", "", "")
    class SenderConnectivity(
        @XmlElement val ChannelObjectId: String,
        @XmlElement val AdapterName: String,
        @XmlElement val AdapterNamespace: String,
        @XmlElement val AdapterSWCV: String,
        @XmlElement val EngineName: String,
        @XmlElement val EngineType: String,
        @XmlElement val InterfaceVersion: String,
        @XmlElement val ValidationMode: Int,
        @XmlElement val VirusScanMode: Int,
        @XmlElement val AllInOneAttributes: AllInOneAttributes,
        //@XmlElement val HeaderMapping: HeaderMapping?,
        @XmlElement val Users: Users?,
    )

    @Serializable
    @XmlSerialName("Users", "", "")
    class Users(
        @XmlElement val Name: List<String>,
    )

    @Serializable
    @XmlSerialName("HeaderMapping", "", "")
    class HeaderMapping(
        @XmlElement val FieldMapping: FieldMapping,
    )

    @Serializable
    @XmlSerialName("FieldMapping", "", "")
    class FieldMapping(
        @XmlElement val Field: List<Field>,
    )

    @Serializable
    @XmlSerialName("Field", "", "")
    class Field(
        @XmlElement val Name: String,
        @XmlElement val Extractor: Extractor,
    )

    @Serializable
    @XmlSerialName("Extractor", "", "")
    class Extractor(
        @XmlElement val ExtractorId: String,
        @XmlElement @XmlSerialName("ExtractorData", "", "") val ExtractorData: AbstractExtractor,
    )

    @Serializable
    @XmlSerialName("AllInOneAttributes", "", "")
    class AllInOneAttributes(
        @XmlElement val AdapterTypeData: AdapterTypeData,
    )

    @Serializable
    @XmlSerialName("NamespaceMapping", "", "")
    class NamespaceMapping(
        @XmlElement val NSM: NSM,
    )

    @Serializable
    @XmlSerialName("NSM", "", "") //"""http://sap.com/xi/ib/prefix", "nsm")
    class NSM(
        @XmlElement @XmlSerialName("definition", "", "") val definition: List<Definition>,
    )

    @Serializable
    @XmlSerialName("definition")
    class Definition(
        val prefix: String,
        val uri: String,
    )

    @Serializable
    @XmlSerialName("NoReceiverBehaviour", "", "")
    class NoReceiverBehaviour(
        @XmlElement val IfNoReceiverFound: Int,
        @XmlElement val Receiver: Receiver?,
        @XmlElement val DefaultReceiverId: String?,
    )

    @Serializable
    @XmlSerialName("ReceiverConfigurations", "", "")
    class ReceiverConfigurations(
        @XmlSerialName("ReceiverConfiguration", "", "") val ReceiverConfiguration: List<ReceiverConfiguration>,
    )

    @Serializable
    class ReceiverConfiguration(
        @XmlElement val ReceiverId: String?,
        @XmlElement val ReceiverConfigurationObjectId: String?,                 //полный кэш
        @XmlElement val Receiver: Receiver,
        @XmlElement val InterfaceDeterminations: InterfaceDeterminations,
    )

    @Serializable
    @XmlSerialName("Receiver", "", "")
    class Receiver(
        @XmlElement @XmlSerialName("ReceiverConditionId", "", "") val ReceiverConditionId: String?, //<empty> для полного кэша, игнорим
        @XmlElement @XmlSerialName("PartyExtractor", "", "") val PartyExtractor: AbstractExtractor,
        @XmlElement @XmlSerialName("PartyAgencyExtractor", "", "") val PartyAgencyExtractor: AbstractExtractor,
        @XmlElement @XmlSerialName("PartySchemaExtractor", "", "") val PartySchemaExtractor: AbstractExtractor,
        @XmlElement @XmlSerialName("ServiceExtractor", "", "") val ServiceExtractor: AbstractExtractor,
    )

    @Serializable
    class AbstractExtractor(
        @XmlElement val TRD_EXTRACTOR: TRD_EXTRACTOR,
    )

    @Serializable
    @XmlSerialName("TRD_EXTRACTOR", "", "")
    class TRD_EXTRACTOR(
        @XmlElement val EXTRACTORID: String,
        @XmlElement val TYPE: String,
        @XmlElement val VALUE: String,
        @XmlElement val DATATYPE: String,
        @XmlElement val MULTILINE: String?,     //у сапа опечатка в выгрузках
        @XmlElement val MULTLINE: String?,
        @XmlElement val COBJNS: String,
        @XmlElement val COBJNAME: String,
    )

    @Serializable
    @XmlSerialName("InterfaceDeterminations", "", "")
    class InterfaceDeterminations(
        @XmlElement @XmlSerialName("InterfaceDetermination", "", "") val InterfaceDetermination: List<InterfaceDetermination>,
    )

    @Serializable
    class InterfaceDetermination(
        @XmlElement val FromOperation: String,
        @XmlElement val FromXmlRoot: String,
        @XmlElement val FromXmlRootNamespace: String,
        @XmlElement val InterfaceConditionId: String,
        @XmlElement val Interfaces: Interfaces?,                //нет в полном
        @XmlElement val IntfMappingName: String,
        @XmlElement val IntfMappingNamespace: String,
        @XmlElement val IntfMappingObjectId: String,
        @XmlElement val IntfMappingSwcvId: String,
        @XmlElement val MappingRequired: Int,
        @XmlElement val InterfaceDeterminationObjectId: String?,    //в полном кэше
        @XmlElement val ReceiverConnectivityId: String?,    //в полном кэше
        @XmlElement val InterfaceName: String?,    //в полном кэше
        @XmlElement val InterfaceNamespace: String?,    //в полном кэше
        @XmlElement val SequenceNumber: Int?,    //в полном кэше
        @XmlElement @XmlSerialName("properties", "", "") val properties: IDProperties?,
    )

    @Serializable
    class IDProperties(
        @XmlElement val property: List<IDProperty>,
    )

    @Serializable
    @XmlSerialName("property", "", "")
    class IDProperty(
        val id: Int,
        val guid: String,
        val name: String,
        val counter: Int,
        val type: String,   //xsd:string
        val valueType: String,
        val direction: String,
        val parentguid: String,
        @XmlValue val value: String,
    )

    @Serializable
    @XmlSerialName("Interfaces", "", "")
    class Interfaces(
        @XmlElement @XmlSerialName("Interface", "", "") val Interface: List<Interface>,
    )

    @Serializable
    class Interface(
        @XmlElement val InterfaceName: String,
        @XmlElement val InterfaceNamespace: String,
        @XmlElement val ReceiverConnectivityId: String,
        @XmlElement val SequenceNumber: Int,
    )

    @Serializable
    @XmlSerialName("ReceiverAssignmentList", "", "")
    class ReceiverAssignmentList(
        val mode: String,
        @XmlElement @XmlSerialName("ReceiverAssignment", "", "") val ReceiverAssignment: List<ReceiverAssignment>,
    )

    @Serializable
    @XmlSerialName("ReceiverAssignment", "", "")
    class ReceiverAssignment(
        @XmlElement val ReceiverConditionId: String?,
        @XmlElement val ReceiverIds: ReceiverIds,
    )

    @Serializable
    @XmlSerialName("ReceiverIds", "", "")
    class ReceiverIds(
        @XmlElement @XmlSerialName("ReceiverId", "", "") val ReceiverId: List<String>,
        //@XmlElement val ReceiverId: List<String>,
    )

    @Serializable
    @XmlSerialName("ReceiverConnectivityList", "", "")
    class ReceiverConnectivityList(
        @XmlElement @XmlSerialName("ReceiverConnectivity", "", "") val ReceiverConnectivity: List<ReceiverConnectivity>,
    )

    @Serializable
    class ReceiverConnectivity(
        @XmlElement val FromPartyName: String,
        @XmlElement val FromServiceName: String,
        @XmlElement val ToPartyName: String,
        @XmlElement val ToServiceName: String,
        @XmlElement val ToInterfaceName: String,
        @XmlElement val ToInterfaceNamespace: String,
        @XmlElement val InterfaceVersion: String,
        @XmlElement val ReceiverConnectivityId: String,
        @XmlElement val ChannelObjectId: String,
        @XmlElement val AdapterName: String,
        @XmlElement val AdapterNamespace: String,
        @XmlElement val AdapterSWCV: String,
        @XmlElement val EngineName: String,
        @XmlElement val EngineType: String,
        @XmlElement val ValidationMode: Int,
        @XmlElement val VirusScanMode: Int,
        @XmlElement val AllInOneAttributes: AllInOneAttributes,
        @XmlElement val HeaderMapping: HeaderMappingExt?,                  // тут путаница для полного и неполного кэша
    )

    @Serializable
    @XmlSerialName("HeaderMapping", "", "")
    class HeaderMappingExt(
        @XmlElement val HeaderMapping: HeaderMapping?,
        @XmlElement val FieldMapping: FieldMapping?,        //костыль для работы обоих кэшей
    )

    @Serializable
    @XmlSerialName("Conditions", "", "")
    class Conditions(
        val RDS_CONDSHORT: List<RDS_CONDSHORT>,
    )

    @Serializable
    @XmlSerialName("RDS_CONDSHORT", "", "")
    class RDS_CONDSHORT(
        @XmlElement val CONDITIONID: String,
        @XmlElement @XmlSerialName("CONDLINE", "", "") val CONDLINE: List<CONDLINE>,
    )

    @Serializable
    class CONDLINE(
        @XmlElement val COMPGROUP: Int,
        @XmlElement val COUNTER: Int,
        @XmlElement @XmlSerialName("COMPOP", "", "") val COMPOP: EOp,
        @XmlElement @XmlSerialName("LEXTRACTOR", "", "") val LEXTRACTOR: AbstractExtractor,
        @XmlElement @XmlSerialName("REXTRACTOR", "", "") val REXTRACTOR: AbstractExtractor,
    )

    @Serializable
    @XmlSerialName("ScenarioConfiguration", "", "")
    class ScenarioConfiguration(
        @XmlElement val Parameters: Parameters,
    )

    @Serializable
    @XmlSerialName("Parameters", "", "")
    class Parameters(
        @XmlElement val parameter: List<Parameter>,
    )

    @Serializable
    @XmlSerialName("Parameter", "", "")
    class Parameter(
        @XmlElement val Name: String,   //stage.conf or logger.conf
        @XmlElement val Value: String,  //BI=3,VI=3,MS=3,AM=1,VO=1  or  BI=3,MS=7,AM=15
    )

    @Serializable
    @XmlSerialName("ServiceInterface", "", "")
    class ServiceInterface(
        val version: String,
        @XmlElement val ObjectId: String,
        @XmlElement val Name: String,
        @XmlElement val Namespace: String,
        @XmlElement val SWCV: String,
        @XmlElement val ContainsSensitiveData: Boolean,
    )

    @Serializable
    @XmlSerialName("MPP_MAP", "", "")
    class MPP_MAP(
        @XmlElement val SMPPMAP3: List<SMPPMAP3>,
    )

    @Serializable
    @XmlSerialName("SMPPMAP3", "", "")
    class SMPPMAP3(
        @XmlElement val ATT_XOP_ENABLED: String,
        @XmlElement val DIRECTION: String,
        @XmlElement val PROP_ID: String,
        @XmlElement val STEP: Int,
        @XmlElement val CONT_ID: String,
        @XmlElement val MAPTYPE: String,
        @XmlElement val VERSION_ID: String,
        @XmlElement val MAPNAME: String,
        @XmlElement val STEP_CONT_ID: String?,
        @XmlElement val MAPREQUIRED: Int,
        @XmlElement val OBJECT_ID: String,
        @XmlElement val STEP_NS: String,
        @XmlElement val VERSION_SP: Int,
        @XmlElement val STEP_OBJECT_ID: String?,
        @XmlElement val STEP_VERSION_SP: Int,
        @XmlElement val MAPNS: String,
        @XmlElement val STEP_VERSION_ID: String,
        @XmlElement val ATT_READ: String,
        @XmlElement val PROG: String,
        @XmlElement val IFMNAME: String?,
        @XmlElement val IFMNS: String?,
        @XmlElement val OFMNAME: String?,
        @XmlElement val OFMNS: String?,
    )

    companion object {
        val parser = XML(Value.module()) {
            defaultPolicy {
                autoPolymorphic = true
            }
        }

        private class NamespaceNormalizingReader(reader: XmlReader) : XmlDelegatingReader(reader) {
            override val namespaceURI: String
                get() = ""
        }

        fun decodeCacheRefreshFromReader(xr: XmlReader) = parser.decodeFromReader<CacheRefresh>(NamespaceNormalizingReader(xr))
        fun decodeChannelFromString(s: String) = parser.decodeFromString<Channel>(s)
        fun decodeAllInOneFromString(s: String) = parser.decodeFromString<AllInOne>(s)
    }
}