package ru.rsug.karlutka.sld

import kotlinx.serialization.Serializable
import java.util.zip.ZipInputStream

// прикладуха поверх CIM
@Suppress("unused")
class SLD_CIM {
    enum class Classes {
        // классы сущностей
        SAP_XIDomain, SAP_XIRemoteAdminService, SAP_XIAdapterFramework, SAP_HTTPServicePort, SAP_XIAdapterService,
        SAP_XIIntegrationDirectory,
        SAP_XIIntegrationRepository,
        SAP_XIIntegrationServer,
        SAP_XIRuntimeManagementServer,  //rwb
        SAP_BusinessSystem,

        // (SAP_XIIntegrationRepository, SAP_XIRuntimeManagementServer, SAP_XIIntegrationServer, SAP_XIAdapterFramework) -> SAP_J2EEEngineCluster
        SAP_XIViewedXISubSystem,

        // SAP_XIRemoteAdminService -> SAP_HTTPServicePort
        SAP_XIRemoteAdminServiceAccessByHTTP,

        // SAP_BusinessSystem -> SAP_XIIntegrationServer
        SAP_XIIntegrationServerLogicalIdentity,

        //SAP_XIDomain -> SAP_XIRuntimeManagementServer
        SAP_XIContainedRuntimeManagementServer,

        SAP_XISubSystem,
        SAP_HostedXIRemoteAdminService,

        // не-иксайное
        SAP_J2EEEngineCluster,           //SAP AS Java
        SAP_ApplicationSystem,

        // для тестов и отладки на котиках
        SAP_StandaloneDotNetSystem,
        SAP_DotNetSystemCluster,
        SAP_DotNetSystemClusterDotNetSystem
        ;

        // Делает INSTANCENAME(CreationClassName, Name)
        fun toInstanceName2(name: String): Cim.INSTANCENAME {
            return Cim.INSTANCENAME(
                this.toString(),
                listOf(Cim.KEYBINDING("CreationClassName", this.toString()), Cim.KEYBINDING("Name", name))
            )
        }

        fun toInstanceName4(parent: Cim.INSTANCENAME, name: String): Cim.INSTANCENAME {
            // parent -- например, что было создано методом toInstanceName2
            return Cim.INSTANCENAME(
                this.toString(),
                listOf(
                    Cim.KEYBINDING("SystemCreationClassName", parent.KEYBINDING.find { it.NAME == "CreationClassName" }!!.KEYVALUE),
                    Cim.KEYBINDING("SystemName", parent.KEYBINDING.find { it.NAME == "Name" }!!.KEYVALUE),
                    Cim.KEYBINDING("CreationClassName", this.toString()),
                    Cim.KEYBINDING("Name", name)
                )
            )
        }
    }

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
        val RuntimeType: String?,
    ) {
        companion object {
            fun from(vno: Cim.VALUE_NAMEDOBJECT): SAP_SoftwareComponent? {
                val ElementTypeID = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "ElementTypeID" }!!.KEYVALUE!!
                val Vendor = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "Vendor" }!!.KEYVALUE!!
                val Name = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "Name" }!!.KEYVALUE!!
                val Version = vno.INSTANCENAME.KEYBINDING.find { it.NAME == "Version" }!!.KEYVALUE!!

                val PPMSNumber = vno.INSTANCE.PROPERTY.find { it.NAME == "PPMSNumber" }?.VALUE
                val Caption = vno.INSTANCE.PROPERTY.find { it.NAME == "Caption" }!!.VALUE!!
                val Description = vno.INSTANCE.PROPERTY.find { it.NAME == "Description" }?.VALUE
                val TechnologyType = vno.INSTANCE.PROPERTY.find { it.NAME == "TechnologyType" }?.VALUE
                val Type = vno.INSTANCE.PROPERTY.find { it.NAME == "Type" }?.VALUE
                val RuntimeType = vno.INSTANCE.PROPERTY.find { it.NAME == "RuntimeType" }?.VALUE
                var guid = vno.INSTANCE.PROPERTY.find { it.NAME == "GUID" }?.VALUE
                return if (guid != null) {
                    guid = guid.replace("-", "")
                    require(guid.length == 32)
                    SAP_SoftwareComponent(
                        ElementTypeID, Vendor, Name, Version, guid, Caption, Type, TechnologyType, PPMSNumber, Description, RuntimeType
                    )
                } else null
            }
        }
    }

    companion object {
        fun messageid() = 12345677
        val sldactive = Cim.LOCALNAMESPACEPATH("sld", "active")

        fun SAPExt_GetObjectServer() = Cim.CIM(
            Cim.MESSAGE(
                messageid(), Cim.SIMPLEREQ(Cim.IMETHODCALL("SAPExt_GetObjectServer", sldactive))
            )
        )

        fun SAPExt_GetObjectServer_resp(x: Cim.CIM) = x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE[0]

        fun getClass(clazz: Classes) = getClass(clazz.toString())
        fun getClass(className: String) = Cim.createSimpleRequest(messageid(), "GetClass", sldactive, "ClassName", Cim.CLASSNAME(className))

        fun enumerateInstances(clazz: Classes, vararg properties: String) = enumerateInstances(clazz.toString(), *properties)

        fun enumerateInstances(className: String, vararg properties: String): Cim.CIM {
            return Cim.createSimpleRequest(
                messageid(), "EnumerateInstances", sldactive, mapOf(
                    "ClassName" to Cim.CLASSNAME(className),
                    "LocalOnly" to "false",
                    "IncludeClassOrigin" to "true",
                    "PropertyList" to Cim.VALUE_ARRAY(*properties)
                )
            )
        }

        fun associators(creation: Classes, name: String, assoc: Classes, result: Classes): Cim.CIM =
            associators(creation.toInstanceName2(name), assoc.toString(), result.toString())

        fun associators(instancename: Cim.INSTANCENAME, assocClass: String, resultClass: String): Cim.CIM {
            return Cim.createSimpleRequest(
                messageid(), "Associators", sldactive, mapOf(
                    "ObjectName" to instancename,
                    "AssocClass" to assocClass,
                    "ResultClass" to resultClass,
                    "IncludeClassOrigin" to "true"
                )
            )
        }

        fun instance(clazz: Classes, properties: Map<String, String>) = Cim.createInstance(clazz.toString(), properties)

        fun createInstance(inst: Cim.INSTANCE) = Cim.createSimpleRequest(messageid(), "CreateInstance", sldactive, "NewInstance", inst)

        fun createInstance(iname: Cim.INSTANCENAME, props: Map<String, String> = mapOf()): Cim.CIM {
            val lst = mutableListOf<Cim.PROPERTY>()
            lst.addAll(iname.KEYBINDING.map { Cim.PROPERTY(it.NAME, it.KEYVALUE!!) })
            lst.addAll(props.map { Cim.PROPERTY(it.key, it.value) })

            val instance = Cim.INSTANCE(iname.CLASSNAME, listOf(), lst)
            return createInstance(instance)
        }

        fun modifyInstance(instancename: Cim.INSTANCENAME, properties: Map<String, String>): Cim.CIM {
            val props = properties.map { Cim.PROPERTY(it.key, it.value) }
            val instance = Cim.INSTANCE(instancename.CLASSNAME, listOf(), props)
            val vni = Cim.VALUE_NAMEDINSTANCE(instancename, instance)
            return Cim.CIM(
                Cim.MESSAGE(
                    messageid(), Cim.SIMPLEREQ(
                        Cim.IMETHODCALL(
                            "ModifyInstance", sldactive, null,
                            listOf(
                                Cim.iparamvalue("ModifiedInstance", vni),
                                Cim.iparamvalue("PropertyList", Cim.VALUE_ARRAY(properties))
                            )
                        )
                    )
                )
            )
        }

        // удаление обычного объекта

        fun deleteInstance(instancename: Cim.INSTANCENAME) = deleteInstance(instancename, "DeleteInstance")
        fun SAPExt_DeleteInstanceWithReferencesIfFound(instancename: Cim.INSTANCENAME) = deleteInstance(instancename, "SAPExt_DeleteInstanceWithReferencesIfFound")
        private fun deleteInstance(instancename: Cim.INSTANCENAME, methodName: String): Cim.CIM {
            return Cim.CIM(
                Cim.MESSAGE(
                    messageid(), Cim.SIMPLEREQ(
                        Cim.IMETHODCALL(
                            methodName, sldactive, null,
                            listOf((Cim.iparamvalue("InstanceName", instancename)))
                        )
                    )
                )
            )
        }

        //удаление ассоциации, но через InstanceName
        fun deleteInstance(instance: Cim.INSTANCE): Cim.CIM {
            val lst = instance.PROPERTY_REFERENCE
            require(lst.size == 2)
            val kb0 = Cim.KEYBINDING(lst[0].NAME, null, listOf(lst[0].VALUE_REFERENCE!!))
            val kb1 = Cim.KEYBINDING(lst[1].NAME, null, listOf(lst[1].VALUE_REFERENCE!!))

            val instancename = Cim.INSTANCENAME(
                instance.CLASSNAME,
                listOf(kb0, kb1)
            )
            return Cim.CIM(
                Cim.MESSAGE(
                    messageid(), Cim.SIMPLEREQ(
                        Cim.IMETHODCALL(
                            "DeleteInstance", sldactive, null,
                            listOf((Cim.iparamvalue("InstanceName", instancename)))
                        )
                    )
                )
            )
        }

        fun referenceNames(instancename: Cim.INSTANCENAME): Cim.CIM {
            return Cim.CIM(
                Cim.MESSAGE(
                    messageid(),
                    Cim.SIMPLEREQ(
                        Cim.IMETHODCALL(
                            "ReferenceNames", sldactive, null, listOf(
                                Cim.iparamvalue("ObjectName", instancename)
                            )
                        )
                    )
                )
            )
        }

        fun associatorNames(instancename: Cim.INSTANCENAME, assocClass: Classes? = null, resultClass: Classes? = null): Cim.CIM {
            val lst = mutableListOf(Cim.iparamvalue("ObjectName", instancename))
            if (assocClass != null) {
                lst.add(Cim.iparamvalue("AssocClass", assocClass.toString()))
            }
            if (resultClass != null) {
                lst.add(Cim.iparamvalue("ResultClass", resultClass.toString()))
            }

            return Cim.CIM(
                Cim.MESSAGE(
                    messageid(),
                    Cim.SIMPLEREQ(
                        Cim.IMETHODCALL(
                            "AssociatorNames", sldactive, null, listOf(
                                Cim.iparamvalue("ObjectName", instancename)
                            )
                        )
                    )
                )
            )
        }

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