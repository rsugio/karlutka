package karlutka.parsers.pi

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("XiTrafo", "urn:sap-com:xi:mapping:xitrafo", "tr")
class XiTrafo(
    @XmlElement(true)
    val JdkVersion: String? = null,
    @XmlSerialName("MetaData", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val MetaData: _MetaData,
    @XmlSerialName("ByteCodeJar", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val ByteCodeJar: _ByteCodeJar,
    @XmlSerialName("SourceCode", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceCode: _SourceCode,
    @XmlSerialName("SourceStructure", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceStructure: String? = null,
    @XmlSerialName("TargetStructure", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val TargetStructure: String? = null,
    @XmlSerialName("Multiplicity", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val Multiplicity: String,
    @XmlSerialName("SourceParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceParameters: _SourceParameters,
    @XmlSerialName("TargetParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val TargetParameters: _TargetParameters,
    @XmlSerialName("AdditionalMetaData", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val AdditionalMetaData: _MetaData
) {
    @Serializable
    class _MetaData(
        @XmlElement(true)
        val blob: Blob
    )
    @Serializable
    class _ByteCodeJar(
        @XmlElement(true)
        val blob: Blob
    )

    @Serializable
    class _SourceCode(
        @XmlElement(true)
        val blob: Blob
    )

    @Serializable
    @XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr")
    class Blob(
        val type: String,
        val zipped: Boolean,
        @XmlValue(true) val content: String
    )

    @Serializable
    @XmlSerialName("mappingtool", "", "")
    class MappingTool(
        val version: String,
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
            val version: String,
            val libstorage: Libstorage,
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
                val container: List<Container> = mutableListOf()
            ) {
                @Serializable
                @XmlSerialName("container", "", "")
                class Container(val key: String)
            }

            @Serializable
            @XmlSerialName("testData", "", "")
            class TestData(
                val instances: Instances?,
                @XmlSerialName("parameters", "", "")
                @Contextual
                val parameters: CompactFragment? = null
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
                ) {
                    @Serializable
                    class FunctionStorage(
                        val version: String,
                        @XmlElement(true)
                        val key: Kex,
                        @XmlElement(true)
                        val classname: String = "",
                        @XmlElement(true)
                        @XmlSerialName("package", "", "")
                        val package_: String = "",
                        @XmlElement(true)
                        val jdkVersion: String? = null,
                        @XmlElement(true)
                        val imports: String = "",
                        @XmlElement(true)
                        val globals: Globals,
                        @XmlElement(true)
                        val init: Init,
                        @XmlElement(true)
                        val cleanup: Cleanup,
                        @XmlElement(true)
                        val functionmodel: List<Functionmodel>,
                        @XmlElement(true)
                        val usedjars: String,
                    ) {

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
                            @XmlElement(true)
                            val signature: Signature = Signature("", null),
                            @XmlElement(true)
                            @XmlSerialName("name", "", "")
                            val name: String,
                            @XmlElement(true)
                            @XmlSerialName("key", "", "")
                            val key: String,
                            @XmlElement(true)
                            @XmlSerialName("tab", "", "")
                            val tab: String,
                            @XmlElement(true)
                            @XmlSerialName("title", "", "")
                            val title: String,
                            @XmlElement(true)
                            @XmlSerialName("uiTitle", "", "")
                            val uiTitle: String,
                            @XmlElement(true)
                            @XmlSerialName("implementation", "", "")
                            val implementation: Implementation = Implementation("udf", "")
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
                }
            }

            @Serializable
            class Transformation(
                @XmlSerialName("brick", "", "")
                val bricks: List<Brick> = mutableListOf(),
                val namespaces: Namespaces
            )

            @Serializable
            @XmlSerialName("brick", "", "")
            class Brick(
                val gid: Int = -1,
                val path: String = "/ns2:Rezult",
                val type: String = "Dst",
                val fname: String? = null,
                val fns: String? = null,
                val object_uid: String? = null,
                val context: String? = null,
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
            ) {
                @Serializable
                @XmlSerialName("arg", "", "")
                class Arg(
                    @XmlElement(false)
                    val pin: Int? = null,
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
                val value: String
            )

            @Serializable
            @XmlSerialName("namespaces", "", "")
            class Namespaces(
                @XmlElement(true)
                val properties: Properties
            )
        }
    }

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
            val key: MappingTool.Kex
        )
    }

    @Serializable
    @XmlSerialName("SourceParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    class _SourceParameters(
        @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "tr")
        @XmlElement(true)
        val Parameters: List<Parameter> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("TargetParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    class _TargetParameters(
        @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "tr")
        @XmlElement(true)
        val Parameters: List<Parameter> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "")
    class Parameter(
        @XmlSerialName("Position", "urn:sap-com:xi:mapping:xitrafo", "")
        @XmlElement(true)
        val Position: String,
        @XmlSerialName("Minoccurs", "urn:sap-com:xi:mapping:xitrafo", "")
        @XmlElement(true)
        val Minoccurs: String,
        @XmlSerialName("Maxoccurs", "urn:sap-com:xi:mapping:xitrafo", "")
        @XmlElement(true)
        val Maxoccurs: String
    )

    companion object {
        private val xitrafoxml = XML {
            indentString = "\t"
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun decodeFromString(sxml: String) = xitrafoxml.decodeFromString<XiTrafo>(sxml)
    }
}