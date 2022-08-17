import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
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
        autoPolymorphic = true
    }

    @Test
    fun datetimetest() {
        val s = "2099-12-31T23:59:59.987+03:00"
        println(XsDateTime(s))

        val a = A(XsDateTime(s), "u")

        val t = xmlserializer.encodeToString(a)
        println(t)
    }
}