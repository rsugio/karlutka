package karlutka.parsers.pi

import karlutka.models.MPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class XICache {
    @Serializable
    @SerialName("CacheRefresh")
    class CacheRefresh(
        val DELETED_OBJECTS: DELETED_OBJECTS? = null,
        val Party: List<Party> = listOf(),
        val Channel: List<Channel> = listOf(),
        val AllInOne: List<AllInOne> = listOf(),
    )

    @Serializable
    @SerialName("DELETED_OBJECTS")
    class DELETED_OBJECTS(
        @XmlElement val SAPXI_OBJECT_KEY: List<SAPXI_OBJECT_KEY> = listOf(),
    )

    @Serializable
    @SerialName("SAPXI_OBJECT_KEY")
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
    )

    @Serializable
    @SerialName("AllInOne")
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
//        @Contextual val ScenarioConfiguration: ScenarioConfiguration,
    )

    @Serializable
    @SerialName("SenderConnectivity")
    class SenderConnectivity(
        @XmlElement val ChannelObjectId: String,
        @XmlElement val AdapterName: String,
        @XmlElement val AdapterNamespace: String,
        @XmlElement val AdapterSWCV: String,
        @XmlElement val EngineName: String,
        @XmlElement val EngineType: String,
        @XmlElement val InterfaceVersion: String,
        @XmlElement val ValidationMode: String,
        @XmlElement val VirusScanMode: String,
        //@XmlElement @Contextual @SerialName("AllInOneAttributes") val AllInOneAttributes: CompactFragment,
    )

    @Serializable
    @SerialName("NamespaceMapping")
    class NamespaceMapping(
        @XmlElement val NSM: NSM,
    )

    @Serializable
    @XmlSerialName("NSM", "http://sap.com/xi/ib/prefix", "nsm")
    class NSM(
        @XmlElement val definition: List<Definition>,
    )

    @Serializable
    @XmlSerialName("definition")
    class Definition(
        val prefix: String,
        val uri: String,
    )

    @Serializable
    @SerialName("NoReceiverBehaviour")
    class NoReceiverBehaviour(
        @XmlElement val IfNoReceiverFound: String
    )

    @Serializable
    @SerialName("ReceiverConfigurations")
    class ReceiverConfigurations()

    @Serializable
    @SerialName("ReceiverAssignmentList")
    class ReceiverAssignmentList()

    @Serializable
    @SerialName("ReceiverConnectivityList")
    class ReceiverConnectivityList()

    @Serializable
    @SerialName("Conditions")
    class Conditions()

//    @Serializable
//    @SerialName("ScenarioConfiguration")
//    class ScenarioConfiguration()

    @Serializable
    @SerialName("ServiceInterface")
    class ServiceInterface(
        val version: String,
        @XmlElement val ObjectId: String,
        @XmlElement val Name: String,
        @XmlElement val Namespace: String,
        @XmlElement val SWCV: String,
        @XmlElement val ContainsSensitiveData: Boolean,
    )

    @Serializable
    @SerialName("MPP_MAP")
    class MPP_MAP(
        @XmlElement val SMPPMAP3: List<SMPPMAP3>,
    )

    @Serializable
    @SerialName("SMPPMAP3")
    class SMPPMAP3(
        @XmlElement val ATT_XOP_ENABLED: String,
        @XmlElement val DIRECTION: String,
        @XmlElement val PROP_ID: String,
        @XmlElement val STEP: Int,
        @XmlElement val CONT_ID: String,
        @XmlElement val MAPTYPE: String,
        @XmlElement val VERSION_ID: String,
        @XmlElement val MAPNAME: String,
        @XmlElement val MAPREQUIRED: Int,
        @XmlElement val OBJECT_ID: String,
        @XmlElement val STEP_NS: String,
        @XmlElement val VERSION_SP: Int,
        @XmlElement val STEP_OBJECT_ID: String,
        @XmlElement val STEP_VERSION_SP: Int,
        @XmlElement val MAPNS: String,
        @XmlElement val STEP_VERSION_ID: String,
        @XmlElement val ATT_READ: String,
        @XmlElement val PROG: String,
    )

    companion object {
        private val xml = XML {
            defaultPolicy {
                ignoreUnknownChildren()
            }
        }

        fun decodeFromReader(xr: XmlReader) = xml.decodeFromReader<CacheRefresh>(xr)
    }
}