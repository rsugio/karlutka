
Тест регистрации:

POST /AdapterFramework/regtest
content-type: text/xml

Запрос:
<scenario>
	<scenname/>
	<scenversion/>
	<sceninst/>
	<component>
		<compname>RegistrationTest</compname>
		<compversion>1.0</compversion>
		<property>
			<propname>action</propname>
			<propvalue>RegisterAppWithSLD</propvalue>
		</property>
		<property>
			<propname>language</propname>
			<propvalue>en</propvalue>
		</property>
	</component>
</scenario>

Ответ:
content-type: text/xml
<scenario>
	<scenname/>
	<scenversion/>
	<sceninst/>
	<component>
		<compname>ActivityLogEntry</compname>
		<property>
			<propname>Message</propname>
			<propvalue>Registration of Adapter Framework with SLD was successful</propvalue>
		</property>
		<property>
			<propname>Type</propname>
			<propvalue>INFO</propvalue>
		</property>
	</component>
</scenario>

