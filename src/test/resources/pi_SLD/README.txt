
curl --location 'http://ld-s-devpih.ao.nlmk:50000/sld/cimom' \
--header 'cimprotocolversion: 1.0' \
--header 'cimoperation: MethodCall' \
--header 'cimmethod: SAPExt_GetObjectServer' \
--header 'cimobject: sld/active' \
--header 'accept-charset: UTF-8' \
--header 'accept-encoding: identity' \
--header 'content-type: application/xml; charset=utf-8' \
--header 'accept: application/xml, text/xml' \

<CIM CIMVERSION="2.0" DTDVERSION="2.0">
    <MESSAGE ID="717882128" PROTOCOLVERSION="1.0">
        <SIMPLEREQ>
            <IMETHODCALL NAME="SAPExt_GetObjectServer">
                <LOCALNAMESPACEPATH>
                    <NAMESPACE NAME="sld"/>
                    <NAMESPACE NAME="active"/>
                </LOCALNAMESPACEPATH>
            </IMETHODCALL>
        </SIMPLEREQ>
    </MESSAGE>
</CIM>


Некоторые из методов SAP SLD помимо SAPExt_GetObjectServer включают:

1. SAPExt_GetSystemData - возвращает данные о системе, включая информацию о версии, операционной системе и конфигурации.

2. SAPExt_GetSystemConnections - возвращает список всех соединений, установленных между системой и другими системами.

3. SAPExt_GetSystemComponents - возвращает список компонентов, установленных на системе, включая приложения и сервисы.

4. SAPExt_GetSystemUsers - возвращает список пользователей, которые имеют доступ к системе.

5. SAPExt_GetSystemEvents - возвращает список событий, связанных с системой, таких как ошибки, предупреждения и т. д.

6. SAPExt_GetSystemAlerts - возвращает список предупреждений, связанных с системой, таких как проблемы с производительностью или недостаточное количество ресурсов.

7. SAPExt_GetSystemLogs - возвращает список журналов, связанных с системой, таких как журналы безопасности или журналы ошибок.

часть методов можно выдрать из CL_WBEM_REQUESTASSEMBLER

'AssociatorNames'
'Associators'
'CreateClass'
'CreateInstance'
'DeleteClass'
'DeleteInstance'
'DeleteQualifier'
'EnumerateClasses'
'EnumerateClassNames'
'EnumerateInstanceNames'
'EnumerateInstances'
'EnumerateQualifiers'
'GetClass'
'GetInstance'
'GetProperty'
'GetQualifier'
'ModifyClass'
'ModifyInstance'
'ReferenceNames'
'References'
'SetProperty'
'SetQualifier'
'SAPExt_CopyInstance'
'SAPExt_MergeInstance'
'SAPExt_CreateOrModifyInstance'
'SAPExt_RenameInstance'
'SAPExt_DeleteClassWithReferences'
'SAPExt_DeleteClassWithDependents'
'SAPExt_DeleteInstanceWithReferences'
'SAPExt_DeleteInstanceWithReferencesIfFound'
'SAPExt_DeleteInstanceIfFound'
'SAPExt_GetInstanceCount'
'SAPExt_ReferenceCount'
'SAPExt_EnumerateInstancesNoSubclasses'
'SAPExt_EnumerateInstanceNamesNoSubclasses'
'SAPExt_GetObjectServer'
'SAPExt_SetProviderSyncTime'
'SAPExt_GetSuperclasses'
'SAPExt_GetSuperclassNames'
'SAPExt_References'
'SAPExt_ReferenceNames'
'SAPExt_WR_ModifyClass'
'SAPExt_WR_ModifyInstance'
'SAPExt_WR_MergeInstance'
'AssocClass'
'AssociatorClass'
'AssociatorRole'
'ClassName'
'DeepInheritance'
'IncludeClassOrigin'
'IncludeQualifiers'
'InstanceName'
'LocalOnly'
'ModifiedClass'
'ModifiedInstance'
'NewClass'
'NewInstance'
'NewValue'
'ObjectName'
'PropertyList'
'PropertyName'
'QualifierDeclaration'
'QualifierName'
'ResultClass'
'ResultRole'
'Role'
'Merge'
'SourceName'
'TargetName'
'TargetInstance'
'AllowExistingTargets'
'AdditionalReferences'
'WeakReferences'
