package karlutka.parsers.pi

import karlutka.models.MPI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class XICache {
    @Serializable
    @XmlSerialName("CacheRefresh", "", "")
    class CacheRefresh(
        val DELETED_OBJECTS: DELETED_OBJECTS? = null,
        val Party: List<Party>,
        val Channel: List<Channel>,
        val AllInOne: List<AllInOne>,
        val ServiceInterface: List<ServiceInterface>,
        val MPP_MAP: List<MPP_MAP>
    )

    @Serializable
    @XmlSerialName("DELETED_OBJECTS", "", "")
    class DELETED_OBJECTS(
        @XmlElement val SAPXI_OBJECT_KEY: List<SAPXI_OBJECT_KEY> = listOf(),
    )

    @Serializable
    @XmlSerialName("SAPXI_OBJECT_KEY", "", "")
    class SAPXI_OBJECT_KEY(
        @XmlElement val OBJECT_ID: String,
        @XmlElement @XmlSerialName("OBJECT_TYPE") val OBJECT_TYPE: MPI.ETypeID,    //TODO переделать на Enum
        @XmlElement val SP_NUMBER: Int?,        // только на репозитории
        @XmlElement val VERSION_ID: String?,     // только на репозитории
    )

    @Serializable
    @XmlSerialName("Party", "urn:sap-com:xi:xiParty", "cp")
    class Party(
        @XmlElement val PartyName: String,
        @XmlElement val PartyObjectId: String,
        @XmlElement val PartyIdentifier: List<PartyIdentifier>,
    )

    @Serializable
    @XmlSerialName("PartyIdentifier", "urn:sap-com:xi:xiParty", "cp")
    class PartyIdentifier(
        @XmlElement val Agency: String,
        @XmlElement val Schema: String,
        @XmlElement val Identifier: String,
    )

    @Serializable
    @XmlSerialName("Channel", "urn:sap-com:xi:xiChannel", "cp")
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
    )

    @Serializable
    @XmlSerialName("ChannelAttributes", "urn:sap-com:xi:xiChannel", "cp")
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
        @XmlElement val Value: String,
    )

    @Serializable
    @XmlSerialName("PipelineAttributes", "urn:sap-com:xi:xiChannel", "cp")
    class PipelineAttributes(
        @XmlElement val PipelineData: PipelineData?,
        @XmlElement val ModuleData: ModuleData?,
    )

    @Serializable
    @XmlSerialName("PipelineData", "urn:sap-com:xi:xiChannel", "cp")
    class PipelineData(
        @XmlElement val PipelineConfig: List<PipelineConfig>,
    )

    @Serializable
    @XmlSerialName("PipelineConfig", "urn:sap-com:xi:xiChannel", "cp")
    class PipelineConfig(
        @XmlElement val Position: Int,
        @XmlElement val ModuleName: String,
        @XmlElement val ModuleType: Char,
        @XmlElement val ModuleNs: String,
    )

    @Serializable
    @XmlSerialName("ModuleData", "urn:sap-com:xi:xiChannel", "cp")
    class ModuleData(
        val ModuleConfig: List<ModuleConfig>,
    )

    @Serializable
    @XmlSerialName("ModuleConfig", "urn:sap-com:xi:xiChannel", "cp")
    class ModuleConfig(
        @XmlElement val ModuleNs: String,
        @XmlElement val ParamName: String,
        @XmlElement val ParamValue: String,
    )

    @Serializable
    @XmlSerialName("AllInOne", "", "")
    class AllInOne(
        val version: String,
        @XmlElement val FromPartyName: String,
        @XmlElement val FromServiceName: String,
        @XmlElement val ToPartyName: String,
        @XmlElement val ToServiceName: String,
        @XmlElement val FromInterfaceName: String,
        @XmlElement val FromInterfaceNamespace: String,
        @XmlElement val AllInOneObjectId: String,
        @XmlElement val ParentURI: String,
        @XmlElement val SenderConnectivity: SenderConnectivity,
        @XmlElement val NamespaceMapping: NamespaceMapping,
        @XmlElement val NoReceiverBehaviour: NoReceiverBehaviour,
        @XmlElement val ReceiverConfigurations: ReceiverConfigurations,
        @XmlElement val ReceiverAssignmentList: ReceiverAssignmentList,
        @XmlElement val ReceiverConnectivityList: ReceiverConnectivityList,
        @XmlElement val Conditions: Conditions,
        @XmlElement val ScenarioConfiguration: ScenarioConfiguration,
    )

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
    @XmlSerialName("NSM", "http://sap.com/xi/ib/prefix", "nsm")
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
        @XmlSerialName("ReceiverConfiguration", "", "")
        val ReceiverConfiguration: List<ReceiverConfiguration>,
    )

    @Serializable
    class ReceiverConfiguration(
        @XmlElement val ReceiverId: String,
        @XmlElement val Receiver: Receiver,
        @XmlElement val InterfaceDeterminations: InterfaceDeterminations,
    )

    @Serializable
    @XmlSerialName("Receiver", "", "")
    class Receiver(
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
        @XmlElement val MULTLINE: String,
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
        @XmlElement val Interfaces: Interfaces,
        @XmlElement val IntfMappingName: String,
        @XmlElement val IntfMappingNamespace: String,
        @XmlElement val IntfMappingObjectId: String,
        @XmlElement val IntfMappingSwcvId: String,
        @XmlElement val MappingRequired: Int,
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
        //@XmlElement @XmlSerialName("ReceiverId", "", "") val ReceiverId: List<String>,
        @XmlElement val ReceiverId: String,
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
    )

    @Serializable
    @XmlSerialName("Conditions", "", "")
    class Conditions(
        val RDS_CONDSHORT: RDS_CONDSHORT?,
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
        @XmlElement val COMPOP: String,
        @XmlElement @XmlSerialName("LEXTRACTOR", "", "") val LEXTRACTOR: AbstractExtractor,
        @XmlElement @XmlSerialName("REXTRACTOR", "", "") val REXTRACTOR: AbstractExtractor,
    )

    @Serializable
    @XmlSerialName("ScenarioConfiguration", "", "")
    class ScenarioConfiguration(
        @XmlElement val Parameters: String,
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
    )

    companion object {
//        private val xml = XML {
//            defaultPolicy {
//                //ignoreUnknownChildren()
//            }
//        }

        fun decodeFromReader(xr: XmlReader) = XML.decodeFromReader<CacheRefresh>(xr)
    }
}