package k5

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("_", "", "")
data class TMP1(
    @XmlElement(true)
    val PartyID: String,
    @XmlElement(true)
    val ComponentID: String,
    @XmlElement(true)
    val ChannelID: String
)