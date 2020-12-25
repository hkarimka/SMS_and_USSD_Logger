package com.enazamusic.smsapp.model

import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat

/*
 * This class represents USSD and SMS entries; it is an element of listView from LoggedDataFragment.
 * For USSD objects field 'smsSender' is always null.
 */

class ListViewElement(
    var id: String, var date: Long, var isSentToServer: Boolean, var direction: Direction,
    var type: Type, var smsSender: String?, var text: String
) : Parcelable {

    fun formatted(): String {
        val fDate = SimpleDateFormat("y-MM-d HH:mm:ss").format(date)
        val fIsSentToServer = if (isSentToServer) {
            "V"
        } else {
            "X"
        }
        val fSmsSender = if (smsSender != null && smsSender?.isNotBlank() == true) {
            "[$smsSender]"
        } else {
            ""
        }
        return "[$fDate] [$fIsSentToServer] [${direction.name}] [${type.name}] $fSmsSender: '$text'"
    }

    enum class Direction {
        IN, OUT
    }

    enum class Type {
        SMS, USSD
    }

    override fun equals(other: Any?): Boolean {
       return (other != null && other is ListViewElement
               && id == other.id
               && date == other.date
               && text == other.text
               && isSentToServer == other.isSentToServer)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + isSentToServer.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString().toBoolean(),
        Direction.valueOf(parcel.readString() ?: ""),
        Type.valueOf(parcel.readString() ?: ""),
        parcel.readString(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeLong(date)
        parcel.writeString(isSentToServer.toString())
        parcel.writeString(direction.name)
        parcel.writeString(type.name)
        parcel.writeString(smsSender)
        parcel.writeString(text)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ListViewElement> {
        override fun createFromParcel(parcel: Parcel): ListViewElement {
            return ListViewElement(parcel)
        }

        override fun newArray(size: Int): Array<ListViewElement?> {
            return arrayOfNulls(size)
        }
    }
}