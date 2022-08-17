
http://host:50000/AdapterFramework/channelAdmin/ChannelAdmin.xsd

https://blogs.sap.com/2020/10/14/automate-it-an-overview-on-sap-pi-po-apis
[7] Channel Status Servlet



http(s)://<host>:<port>/AdapterFramework/ChannelAdminServlet?party=*&service=*&channel=*&action=status&status=error
http(s)://<host>:<port>/AdapterFramework/ChannelAdminServlet?party=*&service=BS_MySystem&channel=CC_SND_FILE_Filepicker&action=stop



action	required	n/a	Controls how api behaves.
	Possible values: status, start, stop
status	optional	all	Allows to filter for channels of a specific state. (Should be used in combination with action=status and any party/service/channel combination.)
	Possible values: all, ok, error, stopped, inactive, unknown, unregistered
showProcessLog	optional	false	When action=status, this controls if last processing log entries are provided.
	Possible values: true, false
showAdminHistory	optional	false	When action=status, this controls if the admin history (=who has stopped, started, etc. a channel) is provided, too.
	Possible values: true, false


Посмотреть полный список:
{{host}}/AdapterFramework/ChannelAdminServlet?party=*&service=*&channel=*SOAP*&action=status&status=all&showProcessLog=true&showAdminHistory=true

Остановить канал (см 03stop.xml):
{{host}}/AdapterFramework/ChannelAdminServlet?party=*&service=BC_TEST1&channel=qqqq&action=stop

