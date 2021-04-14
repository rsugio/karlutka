import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import org.junit.Test


@Serializable
@XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "S")
data class Envelope2<BODYTYPE>(
    @XmlElement(true)
    @Contextual
    @XmlSerialName("Header", "http://schemas.xmlsoap.org/soap/envelope/", "S")
    val header: CompactFragment? = null,
    val body: Body<BODYTYPE>,
) {
    @Serializable
    @XmlSerialName("Body", "http://schemas.xmlsoap.org/soap/envelope/", "S")
    class Body<BODYTYPE>(@Polymorphic val data: BODYTYPE)
}

@Serializable
@XmlSerialName("A", "urn:vendor-com:A", "")
data class A(val a: String, val x: Int = 1)

@Serializable
@XmlSerialName("B", "urn:vendor-com:B", "")
data class B(val b: String, val y: Int = 2)

private val xmlmodule = SerializersModule {
    polymorphic(Any::class) {
        subclass(A::class, serializer())
        subclass(B::class, serializer())
    }
}
private val xmlserializer = XML(xmlmodule) {
    xmlDeclMode = XmlDeclMode.None
    autoPolymorphic = true
}

class SoapTest {
    @Test
    fun demo1() {
        val aEnvelope = Envelope2(null, Envelope2.Body(A("stringA")))
        val aSoap = xmlserializer.encodeToString(aEnvelope)
        println(aSoap)
        val a = xmlserializer.decodeFromString<Envelope2<A>>(aSoap).body.data
        println(a)
        val aSoap2 = """<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Header>123456<a/></S:Header>
            |<S:Body><A xmlns="urn:vendor-com:A" a="stringA" x="1"/></S:Body></S:Envelope>""".trimMargin()
        val ax = xmlserializer.decodeFromString<Envelope2<A>>(aSoap2)
        println(ax)
    }
}