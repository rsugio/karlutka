import karlutka.parsers.PXsd
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test

class KXsdTest {
    @Serializable
    @XmlSerialName("a", "urn:a", "p1")
    data class A(
        @XmlElement(false)
        @XmlValue(true)
        val t: XsDateTime,
        @XmlElement(false)
        val u: String,
    )

    @Serializable
    class XsDateTime() {
        @kotlinx.serialization.Transient
        private var dt: ZonedDateTime? = null

        constructor(xsDateTime: String) : this() {
            require(xsDateTime.isNotEmpty())
            dt = ZonedDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(xsDateTime))
        }

        val value: String
            get() {
                require(dt != null)
                return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt)
            }

        override fun toString(): String {
            return "XsDateTime($value)"
        }
    }

    val xmlmodule = SerializersModule {
    }

    val xmlserializer = XML(xmlmodule) {
        autoPolymorphic = false
    }

    @Test
    fun datetimetest() {
        val s = "2099-12-31T23:59:59.987+03:00"
        println(XsDateTime(s))
        val a = A(XsDateTime(s), "u")
        val t = xmlserializer.encodeToString(a)
        println(t)
    }

    @Test
    fun xsdt() {
        val xs1 = javaClass.getResourceAsStream("/xsd/MT_RouteSend.xsd")
        val xsd1 = PXsd.decodeFromStream(xs1)
        println(xsd1)

        val xs2 = javaClass.getResourceAsStream("/xsd/ComplexTypes.xsd")
        val xsd2 = PXsd.decodeFromStream(xs2)
        println(xsd2)

        val xs3 = javaClass.getResourceAsStream("/xsd/EDIOrdersEntityPOST.xsd")
        val xsd3 = PXsd.decodeFromStream(xs3)
        println(xsd3)

        val xs4 = javaClass.getResourceAsStream("/xsd/SI_GetClaimData_OutSyncService.xsd")
        val xsd4 = PXsd.decodeFromStream(xs4)
        println(xsd4)

        val xs5 = javaClass.getResourceAsStream("/xsd/DESADV.DELVRY07.ZDELVRY07_01.xsd")
        val xsd5 = PXsd.decodeFromStream(xs5)
        println(xsd5)

        val xs6 = javaClass.getResourceAsStream("/xsd/BP2.xsd")
        val xsd6 = PXsd.decodeFromStream(xs6)
        println(xsd6)

    }
}