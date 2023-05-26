// схема единой базы данных
CREATE TABLE IF NOT EXISTS PUBLIC.FAE
(
    SID  CHAR(3) not null primary key,
    INFO VARCHAR not null // просто комментарий для понятности
);

CREATE TABLE IF NOT EXISTS PUBLIC.FAE_CPA
(
    SID    CHAR(3) references PUBLIC.FAE (SID),
    OID    CHAR(32) not null, // ObjectID, guid
    TYPEID VARCHAR  not null, // MPI.ETypeID, справочно
    NAME   VARCHAR  not null, // человекочитаемое имя, для навигации
    XML    VARCHAR  not null, // XML с содержимым объекта
    constraint PK primary key (SID, OID)
);

CREATE TABLE IF NOT EXISTS PUBLIC.FAE_MSG
(
    SID       CHAR(3) references PUBLIC.FAE (SID),
    MESSAGEID CHAR(32),
    DATETIME  VARCHAR,
    SENDER    VARCHAR,
    RECEIVER  VARCHAR,
    BODY      VARCHAR
);

CREATE TABLE IF NOT EXISTS PUBLIC.SWCV
(
    GUID        CHAR(32) not null primary key,
    CAPTION     VARCHAR(64), //при записи из ссылок может ничего не быть
    WS_NAME     VARCHAR(64),
    VENDOR      VARCHAR(32),
    VERSION     VARCHAR(32),
    DESCRIPTION VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS PUBLIC.SRC // источник
(
    NUM    INT auto_increment not null primary key,
    ONLINE BOOL               not null, //true для живых, false для TPZ
    PATH   VARCHAR(128)       not null unique
);

CREATE TABLE IF NOT EXISTS PUBLIC.ESROBJ // объект как идея
(
    NUM    INT auto_increment not null primary key,
    TYPEID VARCHAR(16)        not null,
    OID    CHAR(32)           not null,
    SWCVID CHAR(32) references PUBLIC.SWCV (GUID),
    SWCVSP TINYINT            not null, // -128 to 127 и это кандидат на удаление
    KEY_   VARCHAR(256)       not null, // имя через палки
    constraint EU unique (typeid, oid, swcvid, key_)
);

CREATE TABLE IF NOT EXISTS PUBLIC.ESRVER // версия
(
    NUM    INT auto_increment not null primary key,
    SRCNUM INT references PUBLIC.SRC (NUM),
    OBJNUM INT references PUBLIC.ESROBJ (NUM),
    VID    CHAR(32)           not null,
    TEXT   VARCHAR(256) // текстовое описание
);

CREATE TABLE IF NOT EXISTS PUBLIC.ESRVLINK // ссылки из версии на объекты
(
    VERNUM INT references PUBLIC.ESRVER (NUM),
    ROLE   VARCHAR(16) not null, // возможно сделать enum
    KPOS   INT         not null,
    OBJNUM INT references PUBLIC.ESROBJ (NUM)
);

-- CREATE TABLE IF NOT EXISTS PUBLIC.SPRX_GET_SPROXDAT
-- (
--     SRCNUM INT references PUBLIC.SRC (NUM),
--
-- );


/*
OBJECT	PRX_R3OBJ	CHAR	4	0	Генерация прокси: тип объекта в системе R3 (DTEL, TABL,...)
CLAS	Класс
INTF	Интерфейс
DTEL	Элемент данных
TABL	Структура
TTYP	Тип таблицы
SCON	Семантический контракт
CONT	Контракт
CONI	Реализация контракта
SCEN	Интеграционный сценарий
PAIM	Параметр импорта
PAEX	Парам. экспорта
METH	Метод
FIEL	Поле
ATTR	Атрибут
FAUM	Мэппинг ошибок
REQM	Мэппинг запросов
RESM	Мэппинг ответов
AREQ	Запрос потребителя RFC
ARES	Ответ потребителя RFC
OBJ_NAME	PRX_R3NAME	CHAR	30	0	Генерация прокси: имя объекта в системе SAP
OBJECT1	PRX_R3OBJ	CHAR	4	0	Генерация прокси: тип объекта в системе R3 (DTEL, TABL,...)
OBJ_NAME1	PRX_R3NAME	CHAR	30	0	Генерация прокси: имя объекта в системе SAP
IMPL_CLASS	PRX_IMPL	CHAR	30	0	Генерация прокси: реализующий класс для интерфейса IFR
IFR_TYPE	PRX_IFRTYP	CHAR	20	0	Тип объекта в Intagration Builder
IFR_NSPCE	PRX_NSPCE	CHAR	255	0	Генерация прокси: внешняя область имен
IFR_INTF	PRX_INTFID	CHAR	120	0	Генерация прокси: ид. интерфейса
IFR_OPERATION	PRX_IFRNAM	CHAR	120	0	Генерация прокси: внешнее имя
IFR_GNSPCE	PRX_NSPCE	CHAR	255	0	Генерация прокси: внешняя область имен
GEN_VERS	PRX_GVERS	NUMC	4	0	Генерация прокси: версия генерации
IFR_IDEMPOTENT	PRX_IDEMPOTENT	CHAR	1	0	Операция является idempotent
CHANGED_ON	PRX_CHON	DEC	15	0	Генерация прокси: момент последнего изменения

 */


// RFC SPRX_GET_SPROXDAT читает прокси