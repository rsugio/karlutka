package k1

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.nio.file.Files
import java.nio.file.Paths

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

@Serializable
@XmlSerialName("SourceParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
class SourceParameters(
    @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val Parameters: List<Parameter> = mutableListOf()
)

@Serializable
@XmlSerialName("TargetParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
class TargetParameters(
    @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val Parameters: List<Parameter> = mutableListOf()
)


@Serializable
@XmlSerialName("XiTrafo", "urn:sap-com:xi:mapping:xitrafo", "tr")
class XiTrafo(
    @XmlSerialName("MetaData", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val MetaData: MappingTool,
    @XmlSerialName("ByteCodeJar", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val ByteCodeJar: String,
    @XmlSerialName("SourceStructure", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceStructure: String,
    @XmlSerialName("TargetStructure", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val TargetStructure: String,
    @XmlSerialName("Multiplicity", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val Multiplicity: String,
    @XmlSerialName("SourceParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    val SourceParameters: SourceParameters,
    @XmlSerialName("TargetParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    val TargetParameters: TargetParameters
)

@Serializable
@XmlSerialName("mappingtool", "", "")
class MappingTool(
    val version: String,
    @XmlSerialName("project", "", "")
    val project: Project
) {
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

val xml2 = XML() {
    indentString = "\t"
    xmlDeclMode = XmlDeclMode.None
    autoPolymorphic = true
}

fun main() {
    val functionmodel = MappingTool.Project.Libstorage.Entry.FunctionStorage.Functionmodel(
        MappingTool.Project.Libstorage.Entry.FunctionStorage.Signature("", null),
        "name",
        "key",
        "tab",
        "title",
        "uiTitle",
        MappingTool.Project.Libstorage.Entry.FunctionStorage.Implementation("udf", "a")
    )
    val libref = MappingTool.LibRef(
        MappingTool.LibRef.Ref(
            "UsedFuncLib",
            1,
            MappingTool.Kex(
                MappingTool.Key(
                    "FUNC_LIB",
                    null,
                    "0873b9e8408131c2877d0afba62f942e",
                    listOf("XI30Processing", "urn:cscentr:po:al:Common:XI30Processing")
                )
            )
        )
    )

    val functionStorage = MappingTool.Project.Libstorage.Entry.FunctionStorage(
        "XI7.1",
        MappingTool.Kex(
            MappingTool.Key(
                "FUNC_LIB",
                null,
                null,
                listOf("LocalUserFunctions", "http://com.sap.xi")
            )
        ),
        "_C4C_S4_ExternalSalesDocumentDataQuerySync_req_",
        "com.sap.xi.tf",
        null,
        "import com.sap.aii.mapping.api.*;",
        MappingTool.Project.Libstorage.Entry.FunctionStorage.Globals("javaText"),
        MappingTool.Project.Libstorage.Entry.FunctionStorage.Init(functionmodel),
        MappingTool.Project.Libstorage.Entry.FunctionStorage.Cleanup("?"),
        listOf(functionmodel, functionmodel),
        ""
    )

    val vmdefault = MappingTool.Project.Brick.Bindings.Param(
        "vmdefault", CompactFragment("unknown")
    )
    val bindings = MappingTool.Project.Brick.Bindings(
        listOf(vmdefault, vmdefault)
    )
    val brick = MappingTool.Project.Brick(
        1, "/x:To", "Dst",
        null, null, null, null,
        MappingTool.Project.Brick.ViewData(123, 456),
        bindings,
        MappingTool.Project.Brick.Arg(2, MappingTool.Project.Brick(2)),
        MappingTool.Project.Properties(mutableListOf()),
        MappingTool.Project.Brick.Group(null)
    )
    var mappingTool = MappingTool(
        "XI7.1", MappingTool.Project(
            "XI7.1",
            MappingTool.Project.Libstorage(
                listOf(
                    MappingTool.Project.Libstorage.Entry("usernamespace", functionStorage, null),
                    MappingTool.Project.Libstorage.Entry("XI30Processing", null, libref)
                )
            ),
            MappingTool.Project.Transformation(
                listOf(brick, brick, brick),
                MappingTool.Project.Namespaces(
                    MappingTool.Project.Properties(listOf(MappingTool.Project.Property("1", "2")))
                )
            ),
            MappingTool.Project.TestData(
                MappingTool.Project.Instances(
                    "Test 1",
                    mutableListOf(
                        MappingTool.Project.TestCase("Test 1", "aaa"),
                        MappingTool.Project.TestCase("Test 2", "bbb")
                    )
                )
            )
        )
    )
    var s = xml2.encodeToString(MappingTool.serializer(), mappingTool)
    s = Files.readString(Paths.get("C:\\workspace\\Karlutka\\src\\test\\resources\\mmap\\mappingTool.xml"))
    s = Files.readString(Paths.get("C:\\workspace\\Karlutka\\src\\test\\resources\\mmap\\1.mmap.xml"))
    mappingTool = xml.decodeFromString(s)

}