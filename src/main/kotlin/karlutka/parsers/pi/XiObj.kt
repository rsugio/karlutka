package karlutka.parsers.pi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("xiObj", "urn:sap-com:xi", "xi")
class XiObj (
    @XmlElement(true)
    val idInfo: IdInfo,
    @XmlElement(true)
    val documentation: String? = null,
//    @XmlElement(true)
//    val generic: String,
) {
    @Serializable
    @XmlSerialName("idInfo", "urn:sap-com:xi", "xi")
    class IdInfo(
        val VID: String,
        @XmlElement(true)
        @XmlSerialName("vc", "urn:sap-com:xi", "xi")
        val vc: PCommon.VC,
        @XmlElement(true)
        @XmlSerialName("key", "urn:sap-com:xi", "xi")
        val key: PCommon.Key,
        @XmlElement(true)
        @XmlSerialName("version", "urn:sap-com:xi", "")
        val version: String? = null
    )

    companion object {
        private val xiobjxml = SerializersModule {
            polymorphic(Any::class) {
                subclass(Hm.Instance::class, kotlinx.serialization.serializer())
                subclass(String::class, String.serializer())
            }
        }
        val xioserializer = XML(xiobjxml) {
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun decodeFromString(sxml:String): XiObj = xioserializer.decodeFromString(sxml)
    }
}
