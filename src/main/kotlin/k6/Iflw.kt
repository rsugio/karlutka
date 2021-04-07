package k6

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

/**
 * Разбор саповского Iflw
 */
@Serializable
@XmlSerialName("definitions", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class IFlowBpmnDefinitions(
    val id: String,
    val exporter: String? = null,
    val exporterVersion: String? = null,
    val name: String? = null,
    val targetNamespace: String? = null,
    @XmlElement(true)
    val extensionElements: ExtensionElements? = null,
    @XmlElement(true)
    val partnerRole: MutableList<PartnerRole> = mutableListOf(),
    @XmlElement(true)
    val collaboration: Collaboration,
    @XmlElement(true)
    val process: Process,
    @XmlElement(true)
    val BPMNDiagram: BPMNDiagram,
) {
    companion object {
        val xml = XML() {
            xmlDeclMode = XmlDeclMode.None
        }

        fun parse(payloadXml: String) = xml.decodeFromString<IFlowBpmnDefinitions>(payloadXml)
    }
}

@Serializable
@XmlSerialName("partnerRole", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class PartnerRole(
    val id: String,
    val name: String,
    @XmlElement(true)
    @XmlSerialName("participantRef", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val participantRef: String?
)

@Serializable
@XmlSerialName("collaboration", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Collaboration(
    val id: String,
    val name: String,
    @XmlElement(true)
    val documentation: Documentation? = null,
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
    @XmlElement(true)
    val participant: Participant,
    @XmlElement(true)
    @XmlSerialName("messageFlow", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val messageFlow: MutableList<MessageFlow> = mutableListOf(),
)

@Serializable
@XmlSerialName("documentation", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Documentation(
    val id: String,
    val textFormat: String?,
    @XmlValue(true)
    val text: String?
)

@Serializable
@XmlSerialName("extensionElements", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class ExtensionElements(
    @XmlElement(true)
    val extensionElements: MutableList<IflProperty> = mutableListOf(),
)

@Serializable
@XmlSerialName("property", "http:///com.sap.ifl.model/Ifl.xsd", "ifl")
class IflProperty(
    @XmlElement(true)
    @XmlSerialName("key", "", "")
    val key: String,
    @XmlElement(true)
    @XmlSerialName("value", "", "")
    val value: String?
)

@Serializable
@XmlSerialName("participant", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Participant(
    val id: String,
    val name: String,
    @XmlSerialName("type", "http:///com.sap.ifl.model/Ifl.xsd", "ifl")
    val type: String,
    val processRef: String? = null,
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
)

@Serializable
@XmlSerialName("messageFlow", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class MessageFlow(
    val id: String,
    val name: String?,
    val sourceRef: String,
    val targetRef: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements?
)

@Serializable
@XmlSerialName("process", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Process(
    val id: String,
    val name: String?,
    val isExecutable: Boolean?,
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
    @XmlElement(true)
    @XmlSerialName("subProcess", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val subProcess: Process? = null,
    @XmlElement(true)
    val startEvent: StartEvent? = null,
    @XmlElement(true)
    val endEvent: MutableList<EndEvent> = mutableListOf(),
    @XmlElement(true)
    val callActivity: MutableList<CallActivity> = mutableListOf(),
    @XmlElement(true)
    val parallelGateway: MutableList<ParallelGateway> = mutableListOf(),
    @XmlElement(true)
    val exclusiveGateway: MutableList<ExclusiveGateway> = mutableListOf(),
    @XmlElement(true)
    val serviceTask: MutableList<ServiceTask> = mutableListOf(),
    @XmlElement(true)
    val sequenceFlow: MutableList<SequenceFlow> = mutableListOf(),

    )

@Serializable
@XmlSerialName("callActivity", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class CallActivity(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String? = null,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: String? = null,
    @XmlElement(true)
    val standardLoopCharacteristics: StandardLoopCharacteristics? = null,
)

@Serializable
@XmlSerialName("standardLoopCharacteristics", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class StandardLoopCharacteristics(
    val id: String,
    val loopMaximum: Int,
    @XmlElement(true)
    val loopCondition: LoopCondition
)

@Serializable
@XmlSerialName("loopCondition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class LoopCondition(
    val id: String,
    @XmlSerialName("type", "http://www.w3.org/2001/XMLSchema-instance", "xsi")
    val type: String,
    @XmlValue(true)
    val condition: String
)

@Serializable
@XmlSerialName("startEvent", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class StartEvent(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements? = null,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: String,
    @XmlElement(true)
    val errorEventDefinition: ErrorEventDefinition? = null,
    @XmlElement(true)
    val timerEventDefinition: TimerEventDefinition? = null,
    @XmlElement(true)
    @XmlSerialName("messageEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val messageEventDefinition: String? = null
)

@Serializable
@XmlSerialName("errorEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class ErrorEventDefinition(
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
)

@Serializable
@XmlSerialName("escalationEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class EscalationEventDefinition(
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
)

@Serializable
@XmlSerialName("timerEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class TimerEventDefinition(
    val id: String,
    val name: String? = null,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
)

@Serializable
@XmlSerialName("endEvent", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class EndEvent(
    val id: String,
    val name: String?,
    @XmlElement(true)
    val extensionElements: ExtensionElements? = null,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String?,
    @XmlElement(true)
    @XmlSerialName("messageEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val messageEventDefinition: String? = null,
    @XmlElement(true)
    @XmlSerialName("terminateEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val terminateEventDefinition: String? = null,
    @XmlElement(true)
    @XmlSerialName("escalationEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val escalationEventDefinition: EscalationEventDefinition? = null,
)

@Serializable
@XmlSerialName("serviceTask", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class ServiceTask(
    val id: String,
    val name: String,
    val implementation: String?,
    @XmlElement(true)
    val extensionElements: ExtensionElements?,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: String,
)

@Serializable
@XmlSerialName("sequenceFlow", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class SequenceFlow(
    val id: String,
    val name: String? = null,
    val sourceRef: String,
    val targetRef: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements? = null,
    @XmlElement(true)
    @XmlSerialName("conditionExpression", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val conditionExpression: String? = null
)

@Serializable
@XmlSerialName("exclusiveGateway", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class ExclusiveGateway(
    val default: String,
    val id: String,
    val name: String?,
    val gatewayDirection: String?,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String?,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: MutableList<String> = mutableListOf()
)

@Serializable
@XmlSerialName("parallelGateway", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class ParallelGateway(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String?,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: MutableList<String> = mutableListOf()
)

@Serializable
@XmlSerialName("BPMNDiagram", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNDiagram(
    val id: String,
    val name: String,
    val BPMNPlane: BPMNPlane,
    val BPMNLabelStyle: MutableList<BPMNLabelStyle> = mutableListOf(),
)

@Serializable
@XmlSerialName("BPMNLabelStyle", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNLabelStyle(
    val id: String,
    @XmlElement(true)
    val font: Font
) {
    @Serializable
    @XmlSerialName("Font", "http://www.omg.org/spec/DD/20100524/DC", "dc")
    class Font(val name: String, val size: Float)
}

@Serializable
@XmlSerialName("BPMNPlane", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNPlane(
    val id: String,
    val bpmnElement: String,
    @XmlElement(true)
    val bpmnshapes: MutableList<BPMNShape> = mutableListOf(),
    @XmlElement(true)
    val bpmnedges: MutableList<BPMNEdge> = mutableListOf()
)

@Serializable
@XmlSerialName("BPMNShape", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNShape(
    val id: String,
    val bpmnElement: String,
    val isHorizontal: Boolean? = null,
    val isExpanded: Boolean? = null,
    val isMarkerVisible: Boolean? = null,
    @XmlElement(true)
    val bounds: Bounds,
    @XmlElement(true)
    val label: BPMNLabel? = null,
)

@Serializable
@XmlSerialName("BPMNLabel", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNLabel(
    val id: String,
    val labelStyle: String?,
    @XmlElement(true)
    val bounds: Bounds?
)

@Serializable
@XmlSerialName("BPMNEdge", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNEdge(
    val id: String,
    val bpmnElement: String,
    val sourceElement: String? = null,
    val targetElement: String? = null,
    @XmlElement(true)
    val waypoints: MutableList<Waypoint> = mutableListOf(),
    @XmlElement(true)
    val label: BPMNLabel? = null,
)

@Serializable
@XmlSerialName("Bounds", "http://www.omg.org/spec/DD/20100524/DC", "dc")
class Bounds(
    val height: Float,
    val width: Float,
    val x: Float,
    val y: Float
)

@Serializable
@XmlSerialName("waypoint", "http://www.omg.org/spec/DD/20100524/DI", "di")
class Waypoint(
    val x: Float,
    val y: Float,
    @XmlSerialName("type", "http://www.w3.org/2001/XMLSchema-instance", "xsi")
    val type: String
)
