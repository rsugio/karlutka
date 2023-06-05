
https://www.rfc-editor.org/rfc/rfc2387

В запросе надо выставить:

Content-Type	multipart/related; boundary=SAP_867dc846-d766-11ed-c626-000000417ca6_END; type="text/xml"; start="<soap-8670dd5ad76611ed94c2000000417ca6@sap.com>"

А сам запрос такой:


--SAP_867dc846-d766-11ed-c626-000000417ca6_END
Content-ID: <soap-8670dd5ad76611ed94c2000000417ca6@sap.com>
Content-Disposition: attachment;filename="soap-8670dd5ad76611ed94c2000000417ca6@sap.com.xml"
Content-Type: text/xml; charset=utf-8
Content-Description: SOAP

<SOAP:Envelope xmlns:SOAP='http://schemas.xmlsoap.org/soap/envelope/'>
	<SOAP:Header>
		<sap:ReliableMessaging xmlns:sap='http://sap.com/xi/XI/Message/30' SOAP:mustUnderstand='1'>
			<sap:QualityOfService>ExactlyOnce</sap:QualityOfService>
		</sap:ReliableMessaging>
		<sap:DynamicConfiguration xmlns:sap='http://sap.com/xi/XI/Message/30' SOAP:mustUnderstand='1'>
		</sap:DynamicConfiguration>
		<sap:System xmlns:sap='http://sap.com/xi/XI/Message/30' SOAP:mustUnderstand='1'/>
		<sap:HopList xmlns:sap='http://sap.com/xi/XI/Message/30' SOAP:mustUnderstand='1'>
		</sap:HopList>
		<sap:Main xmlns:sap='http://sap.com/xi/XI/Message/30' versionMajor='3' versionMinor='1' SOAP:mustUnderstand='1'>
			<sap:MessageClass>ApplicationMessage</sap:MessageClass>
			<sap:ProcessingMode>asynchronous</sap:ProcessingMode>
			<sap:MessageId>866f5cdc-d766-11ed-90eb-000000417ca6</sap:MessageId>
			<sap:TimeSent>2023-04-10T06:11:26Z</sap:TimeSent>
			<sap:Sender>
				<sap:Party agency='http://sap.com/xi/XI' scheme='XIParty'/>
				<sap:Service>BC_TEST1</sap:Service>
			</sap:Sender>
			<sap:Receiver>
				<sap:Party agency='http://sap.com/xi/XI' scheme='XIParty'/>
				<sap:Service/>
			</sap:Receiver>
			<sap:Interface namespace='urn:none'>SI_OutAsync</sap:Interface>
		</sap:Main>
	</SOAP:Header>
	<SOAP:Body>
		<sap:Manifest xmlns:sap='http://sap.com/xi/XI/Message/30' xmlns:xlink='http://www.w3.org/1999/xlink'>
			<sap:Payload xlink:type='simple' xlink:href='cid:payload-866f7675d76611ed89de000000417ca6@sap.com'>
				<sap:Name>MainDocument</sap:Name>
				<sap:Type>Application</sap:Type>
			</sap:Payload>
		</sap:Manifest>
	</SOAP:Body>
</SOAP:Envelope>

--SAP_867dc846-d766-11ed-c626-000000417ca6_END
Content-ID: <payload-866f7675d76611ed89de000000417ca6@sap.com>
Content-Disposition: attachment;filename="MainDocument.bin"
Content-Type: application/octet-stream
Content-Description: MainDocument

<a/>

--SAP_867dc846-d766-11ed-c626-000000417ca6_END--
