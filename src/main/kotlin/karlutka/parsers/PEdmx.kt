package karlutka.parsers

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

class PEdmx {
    @Serializable
    @XmlSerialName("Edmx", "http://schemas.microsoft.com/ado/2007/06/edmx", "edmx")
    data class Edmx(
        val DataServices: DataServices,
        val Version: String
    )

    @Serializable
    @XmlSerialName("DataServices", "http://schemas.microsoft.com/ado/2007/06/edmx", "edmx")
    class DataServices(
        @XmlSerialName("DataServiceVersion", "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata", "m")
        val DataServiceVersion: String,
        val Schema: Schema
    )

    @Serializable
    @XmlSerialName("Schema", "http://schemas.microsoft.com/ado/2008/09/edm", "m")
    class Schema(
        val Namespace: String,
        val EntityType: List<EntityType>,
        val ComplexType: List<ComplexType>,
        val Association: List<Association>,
        val EntityContainer: List<EntityContainer>
    )

    @Serializable
    @XmlSerialName("EntityType", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class EntityType(
        val Name: String,
        val BaseType: String?,
        @XmlSerialName("HasStream", "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata", "m")
        val HasStream: Boolean?,
        @XmlElement(true)
        val Key: Key?,
        @XmlElement(true)
        val Property: List<Property>,
        @XmlElement(true)
        val NavigationProperty: List<NavigationProperty>
    )

    @Serializable
    @XmlSerialName("Property", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class Property(
        val Name: String,
        val Type: String,
        val Nullable: Boolean,
        val MaxLength: String?,
        val FixedLength: Boolean?
    )

    @Serializable
    @XmlSerialName("Key", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class Key(
        val PropertyRef: List<PropertyRef>
    )

    @Serializable
    @XmlSerialName("NavigationProperty", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class NavigationProperty(
        val Name: String,
        val Relationship: String,
        val FromRole: String,
        val ToRole: String
    )

    @Serializable
    @XmlSerialName("PropertyRef", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class PropertyRef(
        val Name: String
    )

    @Serializable
    @XmlSerialName("ComplexType", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class ComplexType(
        val Name: String,
        val Property: List<Property>
    )

    @Serializable
    @XmlSerialName("Association", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class Association(
        val Name: String,
        val End: List<End>
    )

    @Serializable
    @XmlSerialName("End", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class End(
        val Type: String,
        val Multiplicity: String,
        val Role: String
    )

    @Serializable
    @XmlSerialName("EntityContainer", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class EntityContainer(
        val Name: String,
        @XmlSerialName("IsDefaultEntityContainer", "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata", "")
        val IsDefaultEntityContainer: String,
        @XmlElement(true)
        val EntitySet: List<EntitySet>,
        @XmlElement(true)
        val AssociationSet: List<AssociationSet>,
        @XmlElement(true)
        val FunctionImport: List<FunctionImport>
    )

    @Serializable
    @XmlSerialName("EntitySet", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class EntitySet(
        val Name: String,
        val EntityType: String
    )

    @Serializable
    @XmlSerialName("Parameter", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class Parameter(
        val Name: String,
        val Type: String,
        val Nullable: Boolean?
    )

    @Serializable
    @XmlSerialName("FunctionImport", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class FunctionImport(
        val Name: String,
        val ReturnType: String,
        val EntitySet: String?,
        @XmlSerialName("HttpMethod", "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata", "")
        val HttpMethod: String?,
        @XmlElement(true)
        val Parameter: List<Parameter>
    )

    @Serializable
    @XmlSerialName("AssociationSet", "http://schemas.microsoft.com/ado/2008/09/edm", "")
    class AssociationSet(
        val Name: String,
        val Association: String,
        @XmlElement(true)
        val End: List<End2>
    ) {
        @Serializable
        @XmlSerialName("End", "http://schemas.microsoft.com/ado/2008/09/edm", "")
        class End2(val EntitySet: String, val Role: String)
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    class U: UnknownChildHandler {
        @OptIn(ExperimentalXmlUtilApi::class)
        override fun handleUnknownChildRecovering(
            input: XmlReader,
            inputKind: InputKind,
            descriptor: XmlDescriptor,
            name: QName?,
            candidates: Collection<Any>
        ): List<XML.ParsedData<*>> {
            TODO("Not yet implemented")
        }

    }

    companion object {
        @OptIn(ExperimentalXmlUtilApi::class)
        fun parseEdmx(edm: String, ignoreNonstandard: Boolean = false): Edmx {
            val xml2 = XML() {
                autoPolymorphic = false
                if (ignoreNonstandard) unknownChildHandler = U()
            }
            val o2 = xml2.decodeFromString<Edmx>(edm)
            return o2
        }
    }
}