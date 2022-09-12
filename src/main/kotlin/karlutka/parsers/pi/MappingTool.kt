package karlutka.parsers.pi

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.io.InputStream

@Serializable
@XmlSerialName("mappingtool", "", "")
class MappingTool(
    val version: String?,
    @XmlSerialName("project", "", "")
    val project: Project
) {
    @Serializable
    @XmlSerialName("key", "", "")
    class Key(
        val typeID: String = "",
        val version: String? = null,
        val oid: String? = null,
        val elem: List<String> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("key", "", "")
    class Kex(val key: Key)

    @Serializable
    class Project(
        val version: String?,
        val libstorage: Libstorage?,
        @XmlElement(true)
//        @XmlSerialName("functionstorage", "", "")
        val functionstorage: FunctionStorage?,
        @XmlSerialName("transformation", "", "")
        val transformation: Transformation,
        val testData: TestData,
        @XmlElement(true)
        @XmlSerialName("ViewState", "", "")
        val viewState: String? = null,
        @XmlElement(true)
        val pcont: PCont = PCont(),
    ) {
        @Serializable
        @XmlSerialName("pcont", "", "")
        class PCont(
            val container: List<XiTrafo.Container> = mutableListOf()
        ) {
        }

        @Serializable
        @XmlSerialName("testData", "", "")
        class TestData(
            val instances: Instances?,
            @XmlSerialName("parameters", "", "")
            @Contextual
            val parameters: CompactFragment? = null,
            @XmlElement(true)
            val testCase: List<TestCase> = mutableListOf()
        )

        @Serializable
        @XmlSerialName("instances", "", "")
        class Instances(
            val default: String? = null,
            val testCase: List<TestCase> = mutableListOf()
        )

        @Serializable
        @XmlSerialName("testCase", "", "")
        class TestCase(
            val name: String,
            @XmlValue(true)
            val content: String
        )

        @Serializable
        @XmlSerialName("libstorage", "", "")
        class Libstorage(
            @XmlSerialName("entry", "", "")
            val entry: List<Entry> = mutableListOf(),
        ) {
            @Serializable
            class Entry(
                val name: String,
                @XmlSerialName("functionstorage", "", "")
                val entry: FunctionStorage?,
                @XmlSerialName("libref", "", "")
                val libref: LibRef?
            )
        }

        @Serializable
        @XmlSerialName("functionstorage", "", "")   //возможно придётся убрать имя отсюда
        class FunctionStorage(
            val version: String?,
            @XmlElement(true)
            val key: Kex?,
            @XmlElement(true)
            val classname: String?,
            @XmlElement(true)
            @XmlSerialName("package", "", "")
            val package_: String?,
            @XmlElement(true)
            @XmlSerialName("jdkVersion", "", "")
            val jdkVersion: String?,
            @XmlElement(true)
            val imports: String?,
            @XmlElement(true)
            val globals: Globals?,
            @XmlElement(true)
            val init: Init?,
            @XmlElement(true)
            val cleanup: Cleanup?,
            @XmlElement(true)
            val functionmodel: List<Functionmodel>,
            @XmlElement(true)
            val usedjars: Usedjars?
        ) {
            @Serializable
            @XmlSerialName("usedjars", "", "")
            class Usedjars (
                @XmlElement(true)
                @XmlSerialName("ref", "", "")
                val ref: List<LibRef.Ref>
            )

            @Serializable
            @XmlSerialName("globals", "", "")
            class Globals(
                @XmlElement(true)
                @XmlSerialName("javaText", "", "")
                val javaText: String
            )

            @Serializable
            @XmlSerialName("cleanup", "", "")
            class Cleanup(
                @XmlElement(true)
                @XmlSerialName("javaText", "", "")
                val javaText: String
            )

            @Serializable
            @XmlSerialName("init", "", "")
            class Init(
                @XmlSerialName("functionmodel", "", "")
                val functionmodel: Functionmodel
            )

            @Serializable
            @XmlSerialName("functionmodel", "", "")
            class Functionmodel(
                val classname: String?,
                @XmlElement(true)
                val argcount: Int?,
                @XmlElement(true)
                val signature: Signature?,
                @XmlElement(true)
                @XmlSerialName("name", "", "")
                val name: String,
                @XmlElement(true)
                @XmlSerialName("namespace", "", "")
                val namespace: String?,
                @XmlElement(true)
                @XmlSerialName("key", "", "")
                val key: String?,
                @XmlElement(true)
                @XmlSerialName("tab", "", "")
                val tab: String,
                @XmlElement(true)
                @XmlSerialName("title", "", "")
                val title: String,
                @XmlElement(true)
                @XmlSerialName("viewclassname", "", "")
                val viewclassname: String?,
                @XmlElement(true)
                @XmlSerialName("description", "", "")
                val description: String?,
                @XmlElement(true)
                @XmlSerialName("type", "", "")
                val type: String?,
                @XmlElement(true)
                @XmlSerialName("text", "", "")
                val text: String?,
                @XmlElement(true)
                @XmlSerialName("uiTitle", "", "")
                val uiTitle: String?,
                @XmlElement(true)
                @XmlSerialName("implementation", "", "")
                val implementation: Implementation?,
                @XmlElement(true)
                val imports: Imports?,
            )

            @Serializable
            @XmlSerialName("implementation", "", "")
            class Implementation(
                val type: String = "udf",
                @XmlElement(true)
                @XmlSerialName("javaText", "", "")
                val javaText: String = ""
            )
            @Serializable
            @XmlSerialName("imports", "", "")
            class Imports(
                @XmlElement(true)
                @XmlSerialName("import", "", "")
                val import: List<String>
            )

            @Serializable
            @XmlSerialName("signature", "", "")
            class Signature(
                val cacheType: String = "2",
                @XmlElement(true)
                val argument: Argument?
            )

            @Serializable
            @XmlSerialName("argument", "", "")
            class Argument(
                val jtp: String = "String",
                val nm: String = "id",
                val tp: Int = 0,
                @XmlElement(true)
                val uiTitle: String?
            )

        }

        @Serializable
        class Transformation(
            val encoding: String?,
            @XmlSerialName("brick", "", "")
            val bricks: List<Brick>,
            val namespaces: Namespaces?,
            val mapping: List<Mapping>
        )

        @Serializable
        @XmlSerialName("brick", "", "")
        class Brick(
            val gid: Int?,
            val gId: String?,
            val path: String?,
            val type: String?,
            val fname: String? = null,
            val fns: String? = null,
            val object_uid: String?,
            val context: String?,
            val asXml: String?,
            @XmlElement(true)
            val viewData: ViewData = ViewData(-1, -1),
            @XmlElement(true)
            val bindings: Bindings? = null,
            @XmlElement(true)
            val arg: Arg? = null,
            @XmlElement(true)
            val properties: Properties? = null,
            @XmlElement(true)
            val group: Group? = null,
            @XmlElement(true)
            val parameter: Parameter?,
        ) {
            @Serializable
            @XmlSerialName("arg", "", "")
            class Arg(
                @XmlElement(false)
                val pin: Int? = null,
                val from: Int?,
                @XmlValue(true)
                val arg: Brick
            )

            @Serializable
            @XmlSerialName("viewData", "", "")
            class ViewData(val x: Int = -1, val y: Int = -1)

            @Serializable
            @XmlSerialName("group", "", "")
            class Group(val object_uid: String? = null)

            @Serializable
            @XmlSerialName("bindings", "", "")
            class Bindings(
                @XmlElement(true)
                val param: List<Param> = mutableListOf()
            ) {
                @Serializable
                @XmlSerialName("param", "", "")
                class Param(
                    val name: String,
                    @XmlElement(true)
                    @XmlSerialName("value", "", "")
                    @Contextual
                    val value: CompactFragment
                )
//                @Serializable
//                @XmlSerialName("value", "", "")
//                class Value(
//                    @XmlValue(true)
//                    val value: String? = null,
////                    @XmlElement(true)
////                    val properties: List<Property>? = null
//                )
            }
        }
        @Serializable
        @XmlSerialName("properties", "", "")
        class Properties(
            @XmlElement(true)
            val properties: List<Property> = mutableListOf()
        )

        @Serializable
        @XmlSerialName("property", "", "")
        class Property(
            val name: String,
            @XmlValue(true)
            val value: String?
        )

        @Serializable
        @XmlSerialName("namespaces", "", "")
        class Namespaces(
            @XmlElement(true)
            val properties: Properties
        )
    }

    @Serializable
    @XmlSerialName("parameter", "", "")
    class Parameter(
        @XmlElement(true) val name: String,
        @XmlElement(true) val type: String,
        @XmlElement(true) val default: String?,
        @XmlElement(true) val calend_props: CalendProps?
    )
    @Serializable
    @XmlSerialName("calend_props", "", "")
    class CalendProps(
        @XmlElement(true) val fd: String?,
        @XmlElement(true) val md: String?,
        @XmlElement(true) val le: Boolean?,
    )

    @Serializable
    @XmlSerialName("libref", "", "")
    class LibRef(
        @XmlSerialName("ref", "", "")
        val ref: Ref
    ) {
        @Serializable
        class Ref(
            val role: String,
            val pos: Int,
            @XmlSerialName("key", "", "")
            val key: Kex
        )
    }
    @Serializable
    @XmlSerialName("mapping", "", "")
    class Mapping(
        val classname: String,  // Mapping
        val destPath: String,   // /ns0:HR_GB_EFI_GetMessageBoxResponse
        val model: Model,
        val properties: Project.Properties?
    )
    @Serializable
    @XmlSerialName("model", "", "")
    class Model(
        val classname: String,  // DestinationValue
        val path: String?,
        val name: String?,
        val namespace: String?,
        val x: Int?,
        val y: Int?,
        val context: String?,
        @XmlElement(true)
        val link: Link?,
        @XmlElement(true)
        val argument: List<Argument>,
        @XmlElement(true)
        val parameter: List<Parameter>
    )

    @Serializable
    @XmlSerialName("argument", "", "")
    class Argument(
        val pin: Int,
        val link: Link
    )

    @Serializable
    @XmlSerialName("link", "", "")
    class Link(
        @XmlElement(true)
        val model: Model
    )
//    @Serializable
//    @XmlSerialName("model", "", "")
//    class LinkModel(
//        val classname: String,  // NodeArgument
//        val path: String,
//        val name: String?
//    )

    companion object {
        fun decodeFromStream(ins: InputStream): MappingTool {
            val xr = PlatformXmlReader(ins, "UTF-8")
            return XML.decodeFromReader(xr)
        }
    }
}
