// схема единой базы данных
CREATE TABLE IF NOT EXISTS PUBLIC.SWCV
(
    GUID    CHAR(32)    not null primary key,
    CAPTION VARCHAR(64) not null, //name также атрибут. Просто имя и версия без вендора
    WS_NAME VARCHAR(64),
    VENDOR  VARCHAR(32),
    VERSION VARCHAR(8)
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


