package com.enazamusic.smsapp.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.text.SimpleDateFormat

/*
 * This class represents USSD and SMS entries; it is an element of listView from LoggedDataFragment.
 * For USSD objects field 'smsSender' is always null.
 */

class ListViewElement(
    @SerializedName("message_queue_id") var serverId: String?,
    @SerializedName("timestamp") var date: Long,
    @SerializedName("direction") var direction: Direction,
    @SerializedName("type") var type: Type,
    @SerializedName("destination") var smsSender: String?,
    @SerializedName("text") var text: String
) : Serializable {

    var isSentToServer: Boolean = false

    fun formatted(): String {
        val fDate = SimpleDateFormat("y-MM-d HH:mm:ss").format(date)
        val fIsSentToServer = if (isSentToServer) {
            "<font color='green'>[V]</font>"
        } else {
            "<font color='red'>[X]</font>"
        }
        val fSmsSender = if (smsSender != null && smsSender?.isNotBlank() == true) {
            "[$smsSender]"
        } else {
            ""
        }
        return "[$serverId] [$fDate] $fIsSentToServer " +
                "<font color='blue'>[${direction.name}] [${type.name}] $fSmsSender</font> " +
                ":'$text'"
    }

    enum class Direction {
        IN, OUT
    }

    enum class Type {
        SMS, USSD
    }

    override fun equals(other: Any?): Boolean {
        return (other != null && other is ListViewElement
                && serverId == other.serverId
                && type == other.type
                && direction == other.direction
                && smsSender == other.smsSender
                && date == other.date
                && text == other.text
                && isSentToServer == other.isSentToServer)
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + isSentToServer.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (smsSender?.hashCode() ?: 0)
        result = 31 * result + (serverId?.hashCode() ?: 0)
        return result
    }
}