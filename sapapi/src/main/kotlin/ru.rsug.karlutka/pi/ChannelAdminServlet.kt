package ru.rsug.karlutka.pi

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// Поскольку в ответе может быть или ErrorInformation или валидный ChannelStatusResult / ChannelAdminResult
// то без своего десериализатора пока костыль в парсере, см в компаньоне
class ChannelAdminServlet {
    @Serializable
    @XmlSerialName("ErrorInformation", "", "")
    class ErrorInformationType(
        @XmlElement(true)
        @XmlSerialName("Exception", "", "")
        var exception: String? = null,
        @XmlElement(true)
        @XmlSerialName("Usage", "", "")
        var usage: String? = null,
        @XmlElement(true)
        @XmlSerialName("Description", "", "")
        var description: String,
    ) {
        fun isSuccess() = description.isEmpty()

        companion object {
            fun parse(sxml: String): ErrorInformationType = xmlserializer.decodeFromString(sxml)
        }
    }

    @Serializable
    @XmlSerialName("ChannelStatusResult", "", "")
    class ChannelStatusResult(
        @XmlElement(true)
        val channels: Channels? = null,
    )

    @Serializable
    @XmlSerialName("ChannelAdminResult", "", "")
    class ChannelAdminResult(
        @XmlElement(true)
        val channels: Channels? = null,
    )

    @Serializable
    @XmlSerialName("Channels", "", "")
    class Channels(
        @XmlElement(true)
        val channels: List<ChannelType> = listOf(),
    )

    @Serializable
    @XmlSerialName("Channel", "", "")
    class ChannelType(
        @XmlElement(true)
        val Party: String,
        @XmlElement(true)
        val Service: String,
        @XmlElement(true)
        val ChannelName: String,
        @XmlElement(true)
        val ChannelID: String,  //GUIDType
        @XmlElement(true)
        val AdapterType: String,
        @XmlElement(true)
        val Direction: String,  //DirectionType
        @XmlElement(true)
        val ActivationState: String,    //ActivationStateType
        @XmlElement(true)
        val ChannelState: String? = null,   //ChannelStateType
        @XmlElement(true)
        val Control: String,    //AutomationStateType
        @XmlElement(true)
        val ShortLog: String? = null,
        @XmlElement(true)
        val AdminErrorInformation: String? = null,
        @XmlElement(true)
        val ProcessLog: ProcessLogType? = null,
        @XmlElement(true)
        val AdminHistory: AdminHistoryType? = null,
    )

    @Serializable
    @XmlSerialName("ProcessLog", "", "")
    class ProcessLogType(
        @XmlElement(true)
        @XmlSerialName("LogEntry", "", "")
        val logEntry: List<LogEntryType> = listOf(),
    )

    @Serializable
    class LogEntryType(
        @XmlElement(true)
        val Time: String,
        @XmlElement(true)
        val Node: String,
        @XmlElement(true)
        val Status: String,
        @XmlElement(true)
        val MessageKey: String,
        @XmlElement(true)
        val Text: String,
    )

    @Serializable
    @XmlSerialName("AdminHistory", "", "")
    class AdminHistoryType(
        @XmlElement(true)
        @XmlSerialName("AdminEntry", "", "")
        val adminEntries: List<AdminEntryType> = listOf(),
    )

    @Serializable
    class AdminEntryType(
        @XmlElement(true)
        val Time: String,
        @XmlElement(true)
        val User: String,
        @XmlElement(true)
        val Action: String,
        @XmlElement(true)
        val AdminErrors: AdminErrorsType? = null,
    )

    @Serializable
    @XmlSerialName("AdminErrors", "", "")
    class AdminErrorsType(
        @XmlElement(true)
        @XmlSerialName("ErrorEntry", "", "")
        val errorEntries: List<ErrorEntryType> = listOf(),
    )

    @Serializable
    class ErrorEntryType(
        @XmlElement(true)
        val Time: String,
        @XmlElement(true)
        val Node: String,
        @XmlElement(true)
        val ErrorInfo: String,
    )

    companion object {
        private val xmlmodule = SerializersModule {
        }
        private val xmlserializer = XML(xmlmodule) {
            autoPolymorphic = true
        }

        fun parse(sxml: String, error: ErrorInformationType): List<ChannelType> {
            //TODO костыль переписать на сериализатор, смотрящий на корневой элемент
            error.exception = null
            error.usage = null
            error.description = ""
            return if (sxml.contains("<!DOCTYPE ErrorInformation")) {
                val e = ErrorInformationType.parse(sxml)
                error.exception = e.exception
                error.usage = e.usage
                error.description = e.description
                mutableListOf()
            } else if (sxml.contains("<!DOCTYPE ChannelAdminResult")) {
                val ds = xmlserializer.decodeFromString<ChannelAdminResult>(sxml)
                ds.channels!!.channels
            } else {
                val ds = xmlserializer.decodeFromString<ChannelStatusResult>(sxml)
                ds.channels!!.channels
            }
        }
    }
}