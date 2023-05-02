package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import java.util.zip.ZipInputStream

// прикладуха поверх CIM
class SLD_CIM {

    @Serializable
    class SAP_SoftwareComponent(
        // Первичный ключ: ElementTypeID, Name, Vendor, Version
        val ElementTypeID: String,
        val Vendor: String,
        val Name: String,
        val Version: String,
        val GUID: String,
        val Caption: String,
        val Type: String?,
        val TechnologyType: String?,
        val PPMSNumber: String?,
        val Description: String?,
        val RuntimeType: String?
    ) {
        companion object {
            fun from(vno: Cim.VALUE_NAMEDOBJECT): SAP_SoftwareComponent? {
                val ElementTypeID = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "ElementTypeID" }!!.KEYVALUE
                val Vendor = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "Vendor" }!!.KEYVALUE
                val Name = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "Name" }!!.KEYVALUE
                val Version = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "Version" }!!.KEYVALUE

                val PPMSNumber = vno.INSTANCE.PROPERTY.find { it.NAME == "PPMSNumber" }?.VALUE
                val Caption = vno.INSTANCE.PROPERTY.find { it.NAME == "Caption" }!!.VALUE!!
                val Description = vno.INSTANCE.PROPERTY.find { it.NAME == "Description" }?.VALUE
                val TechnologyType = vno.INSTANCE.PROPERTY.find { it.NAME == "TechnologyType" }?.VALUE
                val Type = vno.INSTANCE.PROPERTY.find { it.NAME == "Type" }?.VALUE
                val RuntimeType = vno.INSTANCE.PROPERTY.find { it.NAME == "RuntimeType" }?.VALUE
                var guid = vno.INSTANCE.PROPERTY.find { it.NAME == "GUID" }?.VALUE
                if (guid != null) {
                    guid = guid.replace("-", "")
                    require(guid.length == 32)
                    return SAP_SoftwareComponent(
                        ElementTypeID, Vendor, Name, Version, guid, Caption, Type, TechnologyType, PPMSNumber, Description, RuntimeType
                    )
                } else return null
            }
        }
    }

    companion object {
        fun messageid() = 12345677

        val sldactive = Cim.LOCALNAMESPACEPATH("sld", "active")

        val SAPExt_GetObjectServer = Cim.CIM(
            Cim.MESSAGE(
                messageid(),
                Cim.SIMPLEREQ(Cim.IMETHODCALL("SAPExt_GetObjectServer", sldactive))
            )
        )
        val GetClass_SAPXIDomain = Cim.CIM(
            Cim.MESSAGE(
                messageid(),
                Cim.SIMPLEREQ(
                    Cim.IMETHODCALL(
                        "GetClass",
                        sldactive,
                        listOf(Cim.IPARAMVALUE("ClassName", null, Cim.JustName("SAP_XIDomain")))
                    )
                )
            )
        )

        val GetClass_CIM_ManagedElement = Cim.CIM(
            Cim.MESSAGE(
                messageid(),
                Cim.SIMPLEREQ(
                    Cim.IMETHODCALL(
                        "GetClass",
                        sldactive,
                        listOf(Cim.IPARAMVALUE("ClassName", null, Cim.JustName("CIM_ManagedElement")))
                    )
                )
            )
        )

        val EnumerateInstances_SAPXIDomain = Cim.CIM(
            Cim.MESSAGE(
                messageid(),
                Cim.SIMPLEREQ(
                    Cim.IMETHODCALL(
                        "EnumerateInstances", sldactive,
                        listOf(
                            Cim.IPARAMVALUE("ClassName", null, Cim.JustName("SAP_XIDomain")),
                            Cim.IPARAMVALUE("LocalOnly", "false"),
                            Cim.IPARAMVALUE("IncludeClassOrigin", "true"),
                            Cim.IPARAMVALUE(
                                "PropertyList", null, null, null,
                                Cim.VALUE_ARRAY("Name", "Caption")
                            ),
                        )
                    )
                )
            )

        )

        val EnumerateInstances_CIM_ManagedElement = Cim.CIM(
            Cim.MESSAGE(
                messageid(),
                Cim.SIMPLEREQ(
                    Cim.IMETHODCALL(
                        "EnumerateInstances",
                        sldactive,
                        listOf(Cim.IPARAMVALUE("ClassName", null, Cim.JustName("CIM_ManagedElement")))
                    )
                )
            )
        )

        fun decodeFromZip(zins: ZipInputStream, callback: (Cim.CIM) -> Unit) {
            // для стандартного экспорта из SAP SLD
            val gn = Regex("export[0-9]+.xml")
            var ze = zins.nextEntry
            while (ze != null) {
                if (ze.name.matches(gn)) {
                    val cim = Cim.decodeFromStream(zins)
                    callback.invoke(cim)
                }
                ze = zins.nextEntry
            }
        }

    }
}