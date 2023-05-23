
Это сервисы по мониторингу, от головы в ноги

1. Сперва голова шлёт POST fae:50000/ProfileProcessor/basic  (SOAP-ENV)

<ns2:getProfiles xmlns='urn:com.sap.aii.af.service.statistic.ws.impl' xmlns:ns2='urn:ProfileProcessorVi' 
xmlns:ns4='urn:ProfileProcessorWsd/ProfileProcessorVi' 
xmlns:ns3='urn:java/lang'>
            <ns2:applicationKey>sap - XPI_STATISTIC - http://sap.com/xi/XI/Message/30</ns2:applicationKey>
            <ns2:active>true</ns2:active>
        </ns2:getProfiles>
На это ноги отвечают:

<SOAP-ENV:Body xmlns:rpl='urn:ProfileProcessorVi'>
        <rpl:getProfilesResponse xmlns:rn0='urn:com.sap.aii.af.service.statistic.ws.impl' 
xmlns:rn1='urn:com.sap.aii.af.service.statistic.ws' 
xmlns:rn2='http://schemas.xmlsoap.org/soap/encoding/' xmlns:rn3='java:sap/standard' xmlns:rn4='urn:java/lang'>
            <Response>
                <rn0:WSProfile>
                    <rn0:activation>2016-11-15T12:30:55.387+00:00</rn0:activation>
                    <rn0:applicationKey>sap - XPI_STATISTIC - http://sap.com/xi/XI/Message/30</rn0:applicationKey>
                    <rn0:profileKey>XPI</rn0:profileKey>
                </rn0:WSProfile>
            </Response>
        </rpl:getProfilesResponse>
    </SOAP-ENV:Body>