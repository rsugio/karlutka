package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.io.InputStream

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

    enum class ErrCodes {
        CIM_OK, CIM_ERR_FAILED, CIM_ERR_ACCESS_DENIED, CIM_ERR_INVALID_NAMESPACE, CIM_ERR_INVALID_PARAMETER,
        CIM_ERR_INVALID_CLASS, CIM_ERR_NOT_FOUND, CIM_ERR_NOT_SUPPORTED, CIM_ERR_CLASS_HAS_CHILDREN,
        CIM_ERR_CLASS_HAS_INSTANCES, CIM_ERR_INVALID_SUPERCLASS, CIM_ERR_ALREADY_EXISTS,
        CIM_ERR_NO_SUCH_PROPERTY, CIM_ERR_TYPE_MISMATCH, CIM_ERR_QUERY_LANGUAGE_NOT_SUPPORTED,
        CIM_ERR_INVALID_QUERY, CIM_ERR_METHOD_NOT_AVAILABLE, CIM_ERR_METHOD_NOT_FOUND,
        _CIM_UNUSED1, _CIM_UNUSED2,
        CIM_ERR_NAMESPACE_NOT_EMPTY, CIM_ERR_INVALID_ENUMERATION_CONTEXT, CIM_ERR_INVALID_OPERATION_TIMEOUT,
        CIM_ERR_PULL_HAS_BEEN_ABANDONED, CIM_ERR_PULL_CANNOT_BE_ABANDONED, CIM_ERR_FILTERED_ENUMERATION_NOT_SUPPORTED,
        CIM_ERR_CONTINUATION_ON_ERROR_NOT_SUPPORTED, CIM_ERR_SERVER_LIMITS_EXCEEDED, CIM_ERR_SERVER_IS_SHUTTING_DOWN
    }

    // ------------------------------------------------------------------ Declaration
    @Serializable
    @XmlSerialName("DECLARATION", "", "")
    class DECLARATION(
        val DECLGROUP: List<DECLGROUP> = listOf(),
        val DECLGROUP_WITHNAME: List<DECLGROUP_WITHNAME> = listOf(),
        val DECLGROUP_WITHPATH: List<DECLGROUP_WITHPATH> = listOf()
    )

    @Serializable
    @XmlSerialName("DECLGROUP", "", "")
    class DECLGROUP(
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH? = null,
        val NAMESPACEPATH: NAMESPACEPATH? = null,
        val QUALIFIER_DECLARATION: List<QUALIFIER_DECLARATION> = listOf(),
        val VALUE_OBJECT: List<VALUE_OBJECT> = listOf()
    )

    @Serializable
    @XmlSerialName("DECLGROUP.WITHNAME", "", "")
    class DECLGROUP_WITHNAME(
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH? = null,
        val NAMESPACEPATH: NAMESPACEPATH? = null,
        val QUALIFIER_DECLARATION: List<QUALIFIER_DECLARATION> = listOf(),  //0..unbounded
        val VALUE_NAMEDOBJECT: List<VALUE_NAMEDOBJECT> = listOf(),  //0..unbounded
    )

    @Serializable
    @XmlSerialName("DECLGROUP.WITHPATH", "", "")
    class DECLGROUP_WITHPATH(
        val VALUE_OBJECTWITHPATH: List<VALUE_OBJECTWITHPATH> = listOf(),
        val VALUE_OBJECTWITHLOCALPATH: List<VALUE_OBJECTWITHLOCALPATH> = listOf(),
    )

    // ----------------------------------------------------------------------- Value elements

    //class VALUE() - для упрощения везде сделан как String
    @Serializable
    @XmlSerialName("VALUE.ARRAY", "", "")
    class VALUE_ARRAY(
        val VALUE: List<String>,
        val VALUE_NULL: List<VALUE_NULL> = listOf()
    ) {
        constructor(vararg s: String) : this(s.toList())
        constructor(s: Map<String, Any>) : this(s.keys.toList())
    }

    @Serializable
    @XmlSerialName("VALUE.REFERENCE", "", "")
    class VALUE_REFERENCE(
        val CLASSPATH: CLASSPATH? = null,
        val LOCALCLASSPATH: LOCALCLASSPATH? = null,
        val CLASSNAME: CLASSNAME? = null,
        val INSTANCEPATH: INSTANCEPATH? = null,
        val LOCALINSTANCEPATH: LOCALINSTANCEPATH? = null,
        val INSTANCENAME: INSTANCENAME? = null,
    )

    @Serializable
    @XmlSerialName("VALUE.REFARRAY", "", "")
    class VALUE_REFARRAY(
        val VALUE_REFERENCE: List<VALUE_REFERENCE> = listOf(),
        val VALUE_NULL: List<VALUE_NULL> = listOf(),
    )

    @Serializable
    @XmlSerialName("VALUE.OBJECT", "", "")
    class VALUE_OBJECT(
        val CLASS: CLASS? = null,
        val INSTANCE: INSTANCE? = null
    )

    @Serializable
    @XmlSerialName("VALUE.NAMEDINSTANCE", "", "")
    class VALUE_NAMEDINSTANCE(
        val INSTANCENAME: INSTANCENAME,
        val INSTANCE: INSTANCE,
    )

    @Serializable
    @XmlSerialName("VALUE.NAMEDOBJECT", "", "")
    class VALUE_NAMEDOBJECT(
        val INSTANCENAME: INSTANCENAME,
        val INSTANCE: INSTANCE
    )

    @Serializable
    @XmlSerialName("VALUE.OBJECTWITHPATH", "", "")
    class VALUE_OBJECTWITHPATH(
        // или CLASSPATH+CLASS или INSTANCEPATH+INSTANCE
        val CLASSPATH: CLASSPATH? = null,
        val CLASS: CLASS? = null,

        val INSTANCEPATH: INSTANCEPATH? = null,
        val INSTANCE: INSTANCE? = null,
    )

    @Serializable
    @XmlSerialName("VALUE.OBJECTWITHLOCALPATH", "", "")
    class VALUE_OBJECTWITHLOCALPATH(
        // или CLASSPATH+CLASS или INSTANCEPATH+INSTANCE
        val LOCALCLASSPATH: LOCALCLASSPATH? = null,
        val CLASS: CLASS? = null,

        val LOCALINSTANCEPATH: LOCALINSTANCEPATH? = null,
        val INSTANCE: INSTANCE? = null,
    )

    @Serializable
    @XmlSerialName("VALUE.NULL", "", "")
    class VALUE_NULL()

    @Serializable
    @XmlSerialName("VALUE.INSTANCEWITHPATH", "", "")
    class VALUE_INSTANCEWITHPATH(
        val INSTANCEPATH: INSTANCEPATH,
        val INSTANCE: INSTANCE
    )

    // ----------------------------------------------------------------------- Naming and location elements

    @Serializable
    @XmlSerialName("NAMESPACEPATH", "", "")
    class NAMESPACEPATH(
        @XmlElement val HOST: String,
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH
    )

    @Serializable
    @XmlSerialName("LOCALNAMESPACEPATH", "", "")
    class LOCALNAMESPACEPATH(
        val NAMESPACE: List<NAMESPACE> = listOf(),
    ) {
        constructor(vararg a: String) : this(a.toList().map { NAMESPACE(it) })
    }

    @Serializable
    @XmlSerialName("NAMESPACE", "", "")
    class NAMESPACE(val NAME: String)

    //class HOST() - для упрощения везде сделан как String
    //class NAMESPACE() - для упрощения везде сделан как String

    @Serializable
    @XmlSerialName("CLASSNAME", "", "")
    class CLASSNAME(
        val NAME: String
    )

    @Serializable
    @XmlSerialName("LOCALCLASSPATH", "", "")
    class CLASSPATH(
        val NAMESPACEPATH: NAMESPACEPATH,
        val CLASSNAME: CLASSNAME,
    )

    @Serializable
    @XmlSerialName("LOCALCLASSPATH", "", "")
    class LOCALCLASSPATH(
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH,
        val CLASSNAME: CLASSNAME
    )

    @Serializable
    @XmlSerialName("INSTANCEPATH", "", "")
    class INSTANCEPATH(
        val NAMESPACEPATH: NAMESPACEPATH,
        val INSTANCENAME: INSTANCENAME
    )

    @Serializable
    @XmlSerialName("LOCALINSTANCEPATH", "", "")
    class LOCALINSTANCEPATH(
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH,
        val INSTANCENAME: INSTANCENAME
    )

    @Serializable
    @XmlSerialName("INSTANCENAME", "", "")
    class INSTANCENAME(
        @XmlElement(false)
        val CLASSNAME: String,
        val KEYBINDING: List<KEYBINDING> = listOf(),
        val KEYVALUE: KEYVALUE? = null,
        val VALUE_REFERENCE: VALUE_REFERENCE? = null,
    )

    @Serializable
    @XmlSerialName("OBJECTPATH", "", "")
    class OBJECTPATH(
        val INSTANCEPATH: INSTANCEPATH?,
        val CLASSPATH: CLASSPATH?
    )

    @Serializable
    @XmlSerialName("KEYBINDING", "", "")
    data class KEYBINDING(
        val NAME: String,
        @XmlElement(true) val KEYVALUE: String? = null,
        val VALUE_REFERENCE: List<VALUE_REFERENCE> = listOf()
    )

    @Serializable
    @XmlSerialName("KEYVALUE", "", "")
    class KEYVALUE(
        val TYPE: String,
        val VALUETYPE: String, //string, boolean, numeric
    )

    // ----------------------------------------------------------------------- Object definition elements
    @Serializable
    @XmlSerialName("CLASS", "", "")
    class CLASS(
        val NAME: String,
        val SUPERCLASS: String?,
        val QUALIFIER: List<QUALIFIER> = listOf(),  //0..unbounded
        val PROPERTY: List<PROPERTY> = listOf(),                    // choice between three PROPERTY
        val PROPERTY_ARRAY: List<PROPERTY_ARRAY> = listOf(),
        val PROPERTY_REFERENCE: List<PROPERTY_REFERENCE> = listOf(),
        val METHOD: List<METHOD> = listOf(),
    )

    @Serializable
    @XmlSerialName("INSTANCE", "", "")
    class INSTANCE(
        //val lang: String? - optional attr
        @XmlElement(false) val CLASSNAME: String,
        val QUALIFIER: List<QUALIFIER> = listOf(),  //0..unbounded
        val PROPERTY: List<PROPERTY> = listOf(),                    // choice between three PROPERTY
        val PROPERTY_ARRAY: List<PROPERTY_ARRAY> = listOf(),
        val PROPERTY_REFERENCE: List<PROPERTY_REFERENCE> = listOf(),
    )

    @Serializable
    @XmlSerialName("QUALIFIER", "", "")
    class QUALIFIER(
        val NAME: String,
        val TYPE: String,
        val PROPAGATED: Boolean?,
        val OVERRIDABLE: Boolean? = true,  //QualifierFlavor
        val TOSUBCLASS: Boolean? = true,
        val TOINSTANCE: Boolean? = false,
        val TRANSLATABLE: Boolean? = false,
        @XmlElement(true)
        val VALUE: String?,
        val VALUE_ARRAY: VALUE_ARRAY?,
    )

    @Serializable
    @XmlSerialName("QUALIFIER.DECLARATION", "", "")
    class QUALIFIER_DECLARATION(
        val NAME: String,
        val TYPE: String,
        val ISARRAY: Boolean = false,
        val ARRAYSIZE: Int? = null,
        val OVERRIDABLE: Boolean? = true,  //QualifierFlavor
        val TOSUBCLASS: Boolean? = true,
        val TOINSTANCE: Boolean? = false,
        val TRANSLATABLE: Boolean? = false,
        val SCOPE: SCOPE? = null,
        @XmlElement(true)
        val VALUE: String?,
        val VALUE_ARRAY: VALUE_ARRAY?,
    )

    @Serializable
    @XmlSerialName("SCOPE", "", "")
    class SCOPE(
        val CLASS: Boolean? = false,
        val ASSOCIATION: Boolean? = false,
        val REFERENCE: Boolean? = false,
        val PROPERTY: Boolean? = false,
        val METHOD: Boolean? = false,
        val PARAMETER: Boolean? = false,
        val INDICATION: Boolean? = false,
    )

    @Serializable
    @XmlSerialName("PROPERTY", "", "")
    class PROPERTY(
        val NAME: String,
        val TYPE: String,
        val CLASSORIGIN: String?,
        val PROPAGATED: Boolean?,
        val EmbeddedObject: String?, //enum instance, token
        val QUALIFIER: List<QUALIFIER>,   //0..unbounded
        @XmlElement(true) val VALUE: String?,
    ) {
        constructor(name: String, value: String) : this(name, "string", null, null, null, listOf(), value)
    }

    @Serializable
    @XmlSerialName("PROPERTY.ARRAY", "", "")
    class PROPERTY_ARRAY(
        val NAME: String,
        val TYPE: String,
        val CLASSORIGIN: String?,
        val PROPAGATED: Boolean? = null,
        val EmbeddedObject: String? = null, //enum instance, token
        val QUALIFIER: List<QUALIFIER> = listOf(),   //0..unbounded
        val VALUE_ARRAY: VALUE_ARRAY? = null,
    )

    @Serializable
    @XmlSerialName("PROPERTY.REFERENCE", "", "")
    class PROPERTY_REFERENCE(
        val NAME: String,
        val REFERENCECLASS: String?,
        val CLASSORIGIN: String? = null,
        val PROPAGATED: Boolean? = null,
        val QUALIFIER: List<QUALIFIER> = listOf(),   //0..unbounded
        val VALUE_REFERENCE: VALUE_REFERENCE? = null,
    )

    @Serializable
    @XmlSerialName("METHOD", "", "")
    class METHOD(
        val NAME: String,
        val TYPE: String,
        val CLASSORIGIN: String?,
        val PROPAGATED: Boolean?,
        val QUALIFIER: List<QUALIFIER> = listOf(),
        val PARAMETER_REFERENCE: List<PARAMETER_REFERENCE> = listOf(),
        val PARAMETER: List<PARAMETER> = listOf(),
        val PARAMETER_ARRAY: List<PARAMETER_ARRAY> = listOf(),
        val PARAMETER_REFARRAY: List<PARAMETER_REFARRAY> = listOf()
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
        val REFERENCECLASS: String? = null,
        val QUALIFIER: List<QUALIFIER> = listOf(),
    )

    @Serializable
    @XmlSerialName("PARAMETER.ARRAY", "", "")
    class PARAMETER_ARRAY(
        val NAME: String,
        val TYPE: String,
        val ARRAYSIZE: Int? = null,
        val QUALIFIER: List<QUALIFIER> = listOf(),
    )

    @Serializable
    @XmlSerialName("PARAMETER.REFARRAY", "", "")
    class PARAMETER_REFARRAY(
        val NAME: String,
        val REFERENCECLASS: String? = null,
        val ARRAYSIZE: Int? = null,
        val QUALIFIER: List<QUALIFIER> = listOf(),
    )

    // ----------------------------------------------------------------------- Message elements
    @Serializable
    @XmlSerialName("MESSAGE", "", "")
    class MESSAGE(
        val ID: Int,
        val SIMPLEREQ: SIMPLEREQ? = null,
        val MULTIREQ: MULTIREQ? = null,
        val SIMPLERSP: SIMPLERSP? = null,
        val MULTIRSP: MULTIRSP? = null,
        val SIMPLEEXPREQ: SIMPLEEXPREQ? = null,
        val MULTIEXPREQ: MULTIEXPREQ? = null,
        val SIMPLEEXPRSP: SIMPLEEXPRSP? = null,
        val MULTIEXPRSP: MULTIEXPRSP? = null,
        val PROTOCOLVERSION: String = "1.0",
    )

    @Serializable
    @XmlSerialName("MULTIREQ", "", "")
    class MULTIREQ(
    )

    @Serializable
    @XmlSerialName("SIMPLEREQ", "", "")
    class SIMPLEREQ(
        val IMETHODCALL: IMETHODCALL? = null,
        val METHODCALL: METHODCALL? = null,     // не видел примеров использования
    )

    @Serializable
    @XmlSerialName("METHODCALL", "", "")
    class METHODCALL(
        val NAME: String,
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH,
    )

    @Serializable
    @XmlSerialName("PARAMVALUE", "", "")
    class PARAMVALUE()

    @Serializable
    @XmlSerialName("IMETHODCALL", "", "")
    class IMETHODCALL(
        val NAME: String,
        val LOCALNAMESPACEPATH: LOCALNAMESPACEPATH,
        val LOCALCLASSPATH: LOCALCLASSPATH? = null,
        val IPARAMVALUE: List<IPARAMVALUE> = listOf()
    )

    @Serializable
    @XmlSerialName("IPARAMVALUE", "", "")
    class IPARAMVALUE(
        val NAME: String,
        @XmlElement
        @XmlSerialName("VALUE", "", "")
        val VALUE: String? = null,
        val VALUE_ARRAY: VALUE_ARRAY? = null,
        val VALUE_REFERENCE: VALUE_REFERENCE? = null,
        val CLASSNAME: CLASSNAME? = null,
        val INSTANCENAME: INSTANCENAME? = null,
        val QUALIFIER_DECLARATION: QUALIFIER_DECLARATION? = null,
        val CLASS: CLASS? = null,
        val INSTANCE: INSTANCE? = null,
        val VALUE_NAMEDINSTANCE: VALUE_NAMEDINSTANCE? = null,
    )

    @Serializable
    @XmlSerialName("MULTIRSP", "", "")
    class MULTIRSP(
    )

    @Serializable
    @XmlSerialName("SIMPLERSP", "", "")
    class SIMPLERSP(
        val IMETHODRESPONSE: IMETHODRESPONSE
    )

    @Serializable
    @XmlSerialName("METHODRESPONSE", "", "")
    class METHODRESPONSE(
//        val NAME: String,
//        val IRETURNVALUE: IRETURNVALUE?,
//        val ERROR: ERROR?
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
    data class ERROR(
        val CODE: String,
        val DESCRIPTION: String? = null,
        val INSTANCE: List<INSTANCE> = listOf()
    )

    class RETURNVALUE()

    @Serializable
    @XmlSerialName("IRETURNVALUE", "", "")
    data class IRETURNVALUE(
        val CLASSNAME: List<CLASSNAME> = listOf(),
        val INSTANCENAME: List<INSTANCENAME> = listOf(),
        @XmlSerialName("VALUE", "", "")
        val VALUE: List<String> = listOf(),
        val VALUE_OBJECTWITHPATH: List<VALUE_OBJECTWITHPATH> = listOf(),
        val VALUE_OBJECTWITHLOCALPATH: List<VALUE_OBJECTWITHLOCALPATH> = listOf(),
        val VALUE_OBJECT: List<VALUE_OBJECT> = listOf(),
        val OBJECTPATH: List<OBJECTPATH> = listOf(),
        val QUALIFIER_DECLARATION: List<QUALIFIER_DECLARATION> = listOf(),
        val VALUE_ARRAY: VALUE_ARRAY? = null,
        val VALUE_REFERENCE: VALUE_REFERENCE? = null,
        val CLASS: List<CLASS> = listOf(),
        val INSTANCE: List<INSTANCE> = listOf(),
        val INSTANCEPATH: List<INSTANCEPATH> = listOf(),
        val VALUE_NAMEDINSTANCE: List<VALUE_NAMEDINSTANCE> = listOf(),
        val VALUE_INSTANCEWITHPATH: List<VALUE_INSTANCEWITHPATH> = listOf(),
    )

    @Serializable
    @XmlSerialName("MULTIEXPREQ", "", "")
    class MULTIEXPREQ()

    @Serializable
    @XmlSerialName("SIMPLEEXPREQ", "", "")
    class SIMPLEEXPREQ()

    @Serializable
    @XmlSerialName("EXPMETHODCALL", "", "")
    class EXPMETHODCALL()

    @Serializable
    @XmlSerialName("MULTIEXPRSP", "", "")
    class MULTIEXPRSP()

    @Serializable
    @XmlSerialName("SIMPLEEXPRSP", "", "")
    class SIMPLEEXPRSP()

    @Serializable
    @XmlSerialName("EXPMETHODRESPONSE", "", "")
    class EXPMETHODRESPONSE()

    @Serializable
    @XmlSerialName("EXPPARAMVALUE", "", "")
    class EXPPARAMVALUE()

    //class ENUMERATIONCONTEXT()  //removed

    @Serializable
    @XmlSerialName("CORRELATOR", "", "")
    class CORRELATOR()

    companion object {

        fun iparamvalue(name: String, value: Any): IPARAMVALUE {
            val x: IPARAMVALUE
            if (value is String) {
                x = IPARAMVALUE(name, value)
            } else if (value is VALUE_ARRAY) {
                x = IPARAMVALUE(name, null, value)
            } else if (value is VALUE_REFERENCE) {
                x = IPARAMVALUE(name, null, null, value)
            } else if (value is CLASSNAME) {
                x = IPARAMVALUE(name, null, null, null, value)
            } else if (value is INSTANCENAME) {
                x = IPARAMVALUE(name, null, null, null, null, value)
            } else if (value is QUALIFIER_DECLARATION) {
                x = IPARAMVALUE(name, null, null, null, null, null, value)
            } else if (value is CLASS) {
                x = IPARAMVALUE(name, null, null, null, null, null, null, value)
            } else if (value is INSTANCE) {
                x = IPARAMVALUE(name, null, null, null, null, null, null, null, value)
            } else if (value is VALUE_NAMEDINSTANCE) {
                x = IPARAMVALUE(name, null, null, null, null, null, null, null, null, value)
            } else {
                TODO()
            }
            return x
        }

        fun createAssociation(clazz: String, propertyRefFrom: PROPERTY_REFERENCE, propertyRefTo: PROPERTY_REFERENCE): INSTANCE {
            return INSTANCE(clazz, listOf(), listOf(), listOf(), listOf(propertyRefFrom, propertyRefTo))
        }

        // в основном для создания ассоциаций
        fun createPropertyReference(name: String, referenceClass: String, instancepath: INSTANCEPATH): PROPERTY_REFERENCE {
            val vr = VALUE_REFERENCE(null, null, null, instancepath)
            return PROPERTY_REFERENCE(name, referenceClass, null, null, listOf(), vr)
        }

        fun createSimpleReq1param(id: Int, methodname: String, localnamespacepath: LOCALNAMESPACEPATH, iparamname: String, instance: INSTANCE): CIM {
            val iparam = iparamvalue(iparamname, instance)
            return CIM(MESSAGE(id, SIMPLEREQ(IMETHODCALL(methodname, localnamespacepath, null, listOf(iparam)))))
        }

        fun decodeFromReader(xr: XmlReader): CIM {
            return XML.decodeFromReader(xr)
        }

        fun decodeFromStream(xr: InputStream): CIM {
            return XML.decodeFromReader(PlatformXmlReader(xr, "UTF-8"))
        }
    }
}
