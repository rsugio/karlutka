Описание: https://github.com/rsugio/karlutka/wiki


### Конфигурирование karla.yaml (соединения и свойства программы)

```yaml
targets:                    # список подключаемых систем
- sid: <SID>                # уникальный идентификатор системы
  text: необязательный текст
  type: ABAP|PIAF|BTPNEO    # тип системы
  # для ABAP
  jco: !!map                # свойства соединения с абапом
    jco.client.codepage: 4110
    jco.client.lang: RU
    jco.client.client: 400
    jco.client.sysnr: 00
    jco.client.ashost: host.erp.company
  # для PIAF
  url: https://po.company:50100
  # auth есть и для ABAP, и для ABAP
  auth: password123         # ссылка на пароль в файле passwd.yaml
    
tmpdir: c:/data/tmp         # место для временных файлов

# свойства http-клиента
httpClientThreads: 4
httpClientConnectionTimeoutMillis: 1234
httpClientRetryOnServerErrors: 2
httpClientLogLevel: NONE #ALL, HEADERS, BODY, INFO, NONE

# свойства http-сервера
httpServerListenPort: 80
httpServerListenAddress: "127.0.0.1"

# база данных H2 для реляционных данных, обязательное
# для данного случая будет создан C:\data\h2.mv.db 
h2connection: "jdbc:h2:c:/data/h2"

# база данных инфлакс для временных рядов, необязательное 
influxdb:
  host: "http://localhost:8086"
  org: test
  bucket: test1
  auth: influx              # ссылка на securityMaterials.id в passwd.yaml
```

### Конфигурирование passwd.yaml (пароли и тд)

```yaml
# где находится кейстор и какой пароль к нему. Обязательно.
keystore:
  path: "c:/data/keystore.jks"
  passwd: "1234"

# перечень логинов-паролей-токенов и тд
securityMaterials:
  - id: password123         # идентификатор пароля, ссылочный из karla.yaml
    text: необязательный текст
    type: basic|apitoken|oauth
    # для basic - логин и пароль
    login: admin
    passwd: "nimda"
    # для apitoken
    token: "XaH4I6eZPM2iXT6fg2OMxSQ=="
    # для oauth
    client_id: 1234567-2321-21211221-12112323434343
    client_secret: hidden
```

## История
* 2023-05-21 v0.2.2 - фокус с отдельных парсеров и генераторов и клиентов API на FAE/FESR
* 2022-08-17 v0.2.0 - выложено на гитхаб.
  https://github.com/rsugio/karlutka/releases/tag/v0.2.0
Недоработки: пароли в открытом виде, кейстор надо создавать снаружи.
Тестирование: на windows и linux.
* 2021г - писалось много парсеров, бессистемно и без UI
