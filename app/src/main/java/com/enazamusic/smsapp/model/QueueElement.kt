package com.enazamusic.smsapp.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class QueueElement(
    @SerializedName("message_queue_id") var queueId: String,
    @SerializedName("type") var type: ListViewElement.Type,
    @SerializedName("destination") var smsDestination: String?,
    @SerializedName("text") var text: String
) : Serializable {

    var isDone: Boolean = false
    var performDate = 0L

    fun formatted(): String {
        val fDestination = if (smsDestination != null) {
            "[$smsDestination]"
        } else {
            ""
        }
        val fIsDone = if (isDone) {
            "<font color='green'>[V]</font>"
        } else {
            "<font color='red'>[X]</font>"
        }
        return "[$queueId] " +
                "<font color='blue'>[${type.name}] $fDestination</font> " +
                ":'$text'"
    }

    override fun equals(other: Any?): Boolean {
        return (other != null && other is QueueElement
                && queueId == other.queueId
                && type == other.type
                && smsDestination == other.smsDestination
                && text == other.text)
    }

    override fun hashCode(): Int {
        var result = queueId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (smsDestination?.hashCode() ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + isDone.hashCode()
        return result
    }

}