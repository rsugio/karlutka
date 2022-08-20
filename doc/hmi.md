# HMI методы

## Репозитарий
### Сервис-метод, версия
Взятое из `/rep/getregisteredhmimethods/int?container=any`.

```text
_subscription
	_subscribe	7.3/*
	_unsubscribe	7.3/*
	editsubscription	7.3/*
	getdcmetadata	7.3/*
	getdcvariants	7.3/*
	getdcvmetadata	7.3/*
	getdeliverychannels	7.3/*
	getemailforuser	7.3/*
	getsubscriptionsforobject	7.3/*
	getsubscriptionsforuser	7.3/*
acl
	default	7.3/*
applcomp
	content_languages	*/*
	level	*/*
	release	*/*
	support_package	*/*
auth
	haspermissions	7.0/0
cads
	default	7.3/*
changelistpartialservice
	_changelistpartialmethod	7.0/0
	_moveacrosschangelist	7.0/0
	retrieveobjectcontainer	7.0/0
checkmmeswcv
	default	7.3/*
checkobjectsserviceid
	changelist	*/*
	checkactiveobject	*/*
	checkobjectsmethodid	*/*
	clusers	*/*
	picchecks	*/*
checkservices
	checkmatchinginterfacesassignmentmethodid	*/*
	checkobjectsmethodid	*/*
classificationservice
	_getclassificationsystemsgreaterthanversion	7.0/0
	_getclassificationsystemvalues	7.0/0
cmstransport
	transport	*/*
compiler_service_id
	compiler_method_id	7.0/0
conntest
	about_last_test	*/*
	cleanup	*/*
	get_last_info	*/*
	start_test	*/*
containeraccess
	getcontainer	*/*
	getcontainers	*/*
contextobjects
	getcontextinformation	*/*
copyobjectservice
	copyobjectmethod	7.31/*
crossreferencehmiservice
	getcrossreferences	7.0/0
customattribute_hmiservice
	_getcustomattribute_allowedvalues	7.3/*
	_getcustomattributes	7.3/*
customtypes
	default	7.31/*
deletecads
	default	7.3/*
esrtypedetail
	default	7.3/*
esrtypes
	default	7.3/*
folderupdateservice
	updatefolderrefmethod	7.0/0
getauthorizations
	default	7.3/*
getprincipals
	default	7.3/*
getregisteredhmimethods
	default	*/*
getxikeyservice
	getxikeymethod	7.0/0
getxiobjects
	default	*/*
goa
	naviquery	*/*
	readobject	*/*
	readsingleobject	*/*
hmi_authorization
	save_authorization	*/*
hmi_cache_refresh_service
	cacherefresh	*/*
hmi_channel_templates
	create	*/*
	delete	*/*
hmi_db_timestamp_service
	gettimestamp	*/*
hmi_mapping
	read_mapping	*/*
hmi_mappingsif
	read_mappingsif	*/*
hmi_operations
	read_operations	*/*
hmi_read_authorization
	read_authactions	*/*
	read_authorization	*/*
	read_principals	*/*
hmi_read_pism_details
	read_pism_details	*/*
hmi_service_swc_manager
	hmi_method_get_underlying	*/*
hmi_user_roles
	user_role_methods	*/*
hmidiag
	failsession	*/*
	info	*/*
	logins	*/*
	session	*/*
	tc.register	*/*
	testrollback	*/*
	tl	*/*
	ts	*/*
interfaceinfo
	getmatchingsifs	*/*
	getrootmap	*/*
	queryissensitive	*/*
	queryissensitiveforall	*/*
	queryissensitiveforset	*/*
internaldocumentationservice
	_getinternaldocumentation	7.0/0
irmf
	currentrelease	7.0/0
	getchanges	7.0/0
isuseresradmin
	default	7.3/*
logindetailshmi
	getdetails	7.0/0
mappingdisplayqueueservice
	executedisplayqueuemethod	7.31/*
mappingtestservice
	executemappingmethod	7.31/*
	executeoperationmappingmethod	7.31/*
	getheaderparameters	7.31/*
processdefinition
	getxmlrep	*/*
publicwsdl
	serviceinterface	*/*
query
	generic	7.0/*
	generic	3.0/*
	generic	2.0/*
	special	*/*
read
	docu	*/*
	options	*/*
	plain	2.0/*
	plain	3.0/*
	plain	7.0/*
	wsdl	2.0/*
	wsdl	3.0/*
	wsdl	7.0/*
	wsdl	7.11/*
	wsdl	7.1/*
	wsdl	7.2/*
	wsdl	7.3/*
	xihtml	*/*
	xim	7.31/*
	xsd	2.0/*
	xsd	3.0/*
	xsd	7.0/*
readcacount
	default	7.3/*
readcad
	default	7.3/*
readcadstate
	default	7.3/*
remoteobjectaccess
	readobjects	*/*
repositoryentity
	default	*/*
rwbnotificationtablerepaccess
	select	*/*
schemacomputation
	getschemadir	*/*
	getschemaundir	*/*
	getserviceinterfaceschemas	7.2/*
setauthorizations
	default	7.3/*
setcadsfortype
	default	7.3/*
solman
	loadmodels	*/*
test
	cleanup	*/*
	import	*/*
texttransport
	getallworkspaces	*/*
	getobject	*/*
	getseqnr	*/*
	getserverversion	*/*
	listobjects	*/*
	setobject	*/*
	translation	*/*
	triggertransport	*/*
transitionconstraintshmiserviceid
	addtransitionshmimethodid	*/*
	getalltransitionshmimethodid	*/*
	removetransitionconstrainthmimethodid	*/*
	removetransitionshmimethodid	*/*
transport_auth_service
	canexecute	7.31/*
transportservice
	read_transport_log	*/*
	read_transport_log_details	*/*
	read_transport_log_log	*/*
	releasetransfer	*/*
	transportmethods	7.31/*
version_history_service
	read_version_history	*/*
	read_version_origin	*/*
webui
	home	*/*
	lockobject	*/*
	readobject	*/*
	saveobject	*/*
	search	*/*
	subscribe	*/*
	unsubscribe	*/*
workspacehmi
	definesupportpackage	7.0/0
	getakhchildren	7.0/0
	getakhroot	7.0/0
	getdestinations	7.0/0
	getidocobjectsfordest	7.0/0
	getisakhsupported	7.0/0
	getrfcmodules	7.0/0
	importr3object	7.0/0
	readsldlinks	7.0/0
	readws	7.0/0
	refreshsldcache	7.0/0
write
	container	*/*
	object	*/*
writecad
	default	7.3/*
```

