package k6

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * Разбор саповского Iflw
 */
@Serializable
@XmlSerialName("definitions", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Definitions(
    val id: String,
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

        fun parse(payloadXml: String) = xml.decodeFromString<Definitions>(payloadXml)
    }
}

@Serializable
@XmlSerialName("collaboration", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Collaboration(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    val participant: Participant,
    @XmlElement(true)
    @XmlSerialName("messageFlow", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val messageFlow: MutableList<MessageFlow> = mutableListOf(),
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
    val value: String
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
    val extensionElements: ExtensionElements,
)

@Serializable
@XmlSerialName("messageFlow", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class MessageFlow(
    val id: String,
    val name: String,
    val sourceRef: String,
    val targetRef: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements
)

@Serializable
@XmlSerialName("process", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class Process(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,


    @XmlElement(true)
    val startEvent: StartEvent?,
    @XmlElement(true)
    val endEvent: MutableList<EndEvent> = mutableListOf(),
    @XmlElement(true)
    val callActivity: MutableList<CallActivity> = mutableListOf(),
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
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: String
)

@Serializable
@XmlSerialName("startEvent", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class StartEvent(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: String,
    @XmlElement(true)
    @XmlSerialName("messageEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val messageEventDefinition: String? = null
)

@Serializable
@XmlSerialName("endEvent", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class EndEvent(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String,
    @XmlElement(true)
    @XmlSerialName("messageEventDefinition", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val messageEventDefinition: String? = null
)

@Serializable
@XmlSerialName("serviceTask", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
class ServiceTask(
    val id: String,
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
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
    val name: String,
    @XmlElement(true)
    val extensionElements: ExtensionElements,
    @XmlElement(true)
    @XmlSerialName("incoming", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val incoming: String,
    @XmlElement(true)
    @XmlSerialName("outgoing", "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2")
    val outgoing: MutableList<String> = mutableListOf()
)

@Serializable
@XmlSerialName("BPMNDiagram", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNDiagram(
    val id: String,
    val name: String,
    val BPMNPlane: BPMNPlane
)

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
    @XmlElement(true)
    val bounds: Bounds
)

@Serializable
@XmlSerialName("BPMNEdge", "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi")
class BPMNEdge(
    val id: String,
    val bpmnElement: String,
    val sourceElement: String? = null,
    val targetElement: String? = null,
    @XmlElement(true)
    val waypoints: MutableList<Waypoint> = mutableListOf()
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
