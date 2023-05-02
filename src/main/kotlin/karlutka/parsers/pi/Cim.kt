package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.io.InputStream
import java.util.zip.ZipInputStream

class Cim {
    @Serializable
    @XmlSerialName("CIM", "", "")
    class CIM(
        val MESSAGE: MESSAGE? = null,           // для обмена по HTTP
        val DECLARATION: DECLARATION? = null,   // для файлов
        val CIMVERSION: String = "2.3",
        val DTDVERSION: String = "2.2",
    ) {
        fun encodeToString() = XML.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("MESSAGE", "", "")
    class MESSAGE(
        val ID: Int,
        val SIMPLEREQ: SIMPLEREQ? = null,
        val MULTIREQ: MULTIREQ? = null,
        val SIMPLERSP: SIMPLERSP? = null,
        val MULTIRSP: MULTIRSP? = null,
        val PROTOCOLVERSION: String = "1.0",
    )

    @Serializable
    @XmlSerialName("SIMPLEREQ", "", "")
    class SIMPLEREQ(
        val IMETHODCALL: IMETHODCALL? = null,
        val METHODCALL: METHODCALL? = null,     // не видел примеров использования
    )

    @Serializable
    @XmlSerialName("MULTIREQ", "", "")
    class MULTIREQ(
    )

    @Serializable
    @XmlSerialName("SIMPLERSP", "", "")
    class SIMPLERSP(
        val IMETHODRESPONSE: IMETHODRESPONSE
    )

    @Serializable
    @XmlSerialName("MULTIRSP", "", "")
    class MULTIRSP(
    )

    @Serializable
    @XmlSerialName("METHODCALL", "", "")
    class METHODCALL(
        val NAME: String,
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH,
    )

    @Serializable
    @XmlSerialName("IMETHODCALL", "", "")
    class IMETHODCALL(
        val NAME: String,
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH,
        val LOCALCLASSPATH: LOCALCLASSPATH? = null,
        val IPARAMVALUE: List<IPARAMVALUE> = listOf()
    )

    @Serializable
    @XmlSerialName("LOCALCLASSPATH", "", "")
    class LOCALCLASSPATH(
        val CLASSNAME: String
    )

    @Serializable
    @XmlSerialName("IPARAMVALUE", "", "")
    class IPARAMVALUE(
        val NAME: String,
        @XmlElement
        @XmlSerialName("VALUE", "", "")
        val VALUE: String? = null,
        @XmlSerialName("CLASSNAME", "", "")
        val CLASSNAME: JustName? = null,
        val INSTANCENAME: INSTANCENAME? = null,
        val VALUE_ARRAY: VALUE_ARRAY? = null,
        val INSTANCE: INSTANCE? = null
    )

    @Serializable
    @XmlSerialName("IMETHODRESPONSE", "", "")
    data class IMETHODRESPONSE(
        val NAME: String,
        val IRETURNVALUE: IRETURNVALUE?,
        val ERROR: ERROR?
    )

    @Serializable
    @XmlSerialName("ERROR", "", "")
    class ERROR(
        val CODE: String,
        val DESCRIPTION: String
    )

    //    <!ELEMENT IRETURNVALUE (CLASSNAME* | INSTANCENAME* | VALUE* | VALUE.OBJECTWITHPATH* | VALUE.OBJECTWITHLOCALPATH* | VALUE.OBJECT* |
    //    OBJECTPATH* | QUALIFIER.DECLARATION* | VALUE.ARRAY? | VALUE.REFERENCE? | CLASS* | INSTANCE* | INSTANCEPATH* | VALUE.NAMEDINSTANCE* |
    //    VALUE.INSTANCEWITHPATH)>
    @Serializable
    @XmlSerialName("IRETURNVALUE", "", "")
    data class IRETURNVALUE(
        //val CLASSNAME:,
        val INSTANCENAME: List<INSTANCENAME> = listOf(),
        @XmlSerialName("VALUE", "", "")
        val VALUE: List<String> = listOf(),
        val VALUE_OBJECTWITHPATH: List<VALUE_OBJECTWITHPATH> = listOf(),
        //val VALUE_OBJECTWITHLOCALPATH: List<VALUE_OBJECTWITHLOCALPATH> = listOf(),
        //val VALUE_OBJECT: List<VALUE_OBJECT> = listOf(),
        //val OBJECTPATH: List<OBJECTPATH> = listOf(),
        val VALUE_NAMEDOBJECT: List<VALUE_NAMEDOBJECT> = listOf(),
        val CLASS: List<CLASS> = listOf(),
        val VALUE_NAMEDINSTANCE: List<VALUE_NAMEDINSTANCE> = listOf(),
        val INSTANCE: List<INSTANCE> = listOf(),
    )

    @Serializable
    @XmlSerialName("LOCALNAMESPACEPATH", "", "")
    class LOCALNAMESPACEPATH(
        @XmlSerialName("NAMESPACE", "", "")
        val NAMESPACE: List<JustName> = listOf(),
    ) {
        constructor(vararg a: String) : this(a.toList().map { JustName(it) })
    }

    @Serializable
    @XmlSerialName("VALUE.OBJECTWITHPATH", "", "")
    class VALUE_OBJECTWITHPATH(
        val INSTANCE: List<INSTANCE> = listOf(),
        val INSTANCEPATH: List<INSTANCEPATH> = listOf()
    )

    @Serializable
    @XmlSerialName("DECLARATION", "", "")
    class DECLARATION(
        val DECLGROUP_WITHNAME: DECLGROUP_WITHNAME
    )

    @Serializable
    @XmlSerialName("DECLGROUP.WITHNAME", "", "")
    class DECLGROUP_WITHNAME(
        val VALUE_NAMEDOBJECT: List<VALUE_NAMEDOBJECT>
    )

    @Serializable
    @XmlSerialName("VALUE.NAMEDOBJECT", "", "")
    class VALUE_NAMEDOBJECT(
        val INSTANCENAME: INSTANCENAME, val INSTANCE: INSTANCE
    )

    @Serializable
    @XmlSerialName("INSTANCENAME", "", "")
    class INSTANCENAME(
        val CLASSNAME: String,
        val KEYBINDING: List<KEYBINDING> = listOf()
    )

    @Serializable
    @XmlSerialName("KEYBINDING", "", "")
    class KEYBINDING(
        val NAME: String,
        @XmlElement(true) val KEYVALUE: String
    )

    @Serializable
    @XmlSerialName("INSTANCE", "", "")
    class INSTANCE(
        val CLASSNAME: String,
        val QUALIFIER: List<QUALIFIER> = listOf(),
        val PROPERTY: List<PROPERTY> = listOf(),
        val PROPERTY_ARRAY: List<PROPERTY_ARRAY> = listOf(),
    )
    @Serializable
    @XmlSerialName("INSTANCEPATH", "", "")
    class INSTANCEPATH(
        val NAMESPACEPATH: NAMESPACEPATH,
        val INSTANCENAME: INSTANCENAME
    )
    @Serializable
    @XmlSerialName("NAMESPACEPATH", "", "")
    class NAMESPACEPATH(
        @XmlElement val HOST: String,
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH
    )

    @Serializable
    @XmlSerialName("QUALIFIER", "", "")
    class QUALIFIER(
        val NAME: String,
        val TYPE: String,
        val TOSUBCLASS: Boolean?,
        val TOINSTANCE: Boolean?,
        val TRANSLATABLE: Boolean?,
        val PROPAGATED: Boolean?,
        val OVERRIDABLE: Boolean?,
        @XmlElement(true)
        val VALUE: String?,
        val VALUE_ARRAY: VALUE_ARRAY?,
    )

    @Serializable
    @XmlSerialName("PROPERTY", "", "")
    class PROPERTY(
        val NAME: String,
        val TYPE: String,
        val PROPAGATED: Boolean? = null,
        val CLASSORIGIN: String? = null,
        @XmlElement(true) val VALUE: String?,
        val QUALIFIER: List<QUALIFIER> = listOf()
    )

    @Serializable
    @XmlSerialName("PROPERTY.ARRAY", "", "")
    class PROPERTY_ARRAY(
        val NAME: String,
        val TYPE: String,
        val CLASSORIGIN: String?,
        val PROPAGATED: Boolean? = null,
        @XmlElement(true) val VALUE: String?,
        val VALUE_ARRAY: VALUE_ARRAY? = null,
    )

    @Serializable
    @XmlSerialName("VALUE.ARRAY", "", "")
    class VALUE_ARRAY(
        val VALUE: List<String> = listOf()
    ) {
        constructor(vararg s: String) : this(s.toList())
    }

    @Serializable
    @XmlSerialName("CLASS", "", "")
    class CLASS(
        val NAME: String,
        val SUPERCLASS: String?,
        val QUALIFIER: List<QUALIFIER> = listOf(),
        val METHOD: List<METHOD> = listOf(),
        val PROPERTY: List<PROPERTY> = listOf()
    )

    @Serializable
    @XmlSerialName("METHOD", "", "")
    class METHOD(
        val NAME: String,
        val TYPE: String,
        val PROPAGATED: Boolean,
        val QUALIFIER: List<QUALIFIER> = listOf(),
        val PARAMETER: List<PARAMETER> = listOf(),
        val METHOD: List<METHOD> = listOf(),
        val PARAMETER_REFERENCE: List<PARAMETER_REFERENCE> = listOf()
    )

    @Serializable
    @XmlSerialName("PARAMETER", "", "")
    class PARAMETER(
        val NAME: String,
        val TYPE: String,
        val QUALIFIER: List<QUALIFIER> = listOf(),
    )

    @Serializable
    @XmlSerialName("PARAMETER.REFERENCE", "", "")
    class PARAMETER_REFERENCE(
        val NAME: String,
        val REFERENCECLASS: String,
        val QUALIFIER: List<QUALIFIER> = listOf(),
    )

    @Serializable
    @XmlSerialName("VALUE.NAMEDINSTANCE", "", "")
    class VALUE_NAMEDINSTANCE(
        val INSTANCENAME: INSTANCENAME,
        val INSTANCE: INSTANCE,
    )

    @Serializable
    class JustName(
        val NAME: String
    )

    companion object {
        fun decodeFromReader(xr: XmlReader): CIM {
            return XML.decodeFromReader(xr)
        }

        fun decodeFromStream(xr: InputStream): CIM {
            return XML.decodeFromReader(PlatformXmlReader(xr, "UTF-8"))
        }

    }
}
