package com.enazamusic.smsapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.enazamusic.smsapp.model.LastTransferAttempt
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.model.QueueElement
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

object Prefs : KoinComponent {
    private val context: Context by inject()
    private const val PREFS_NAME = "ENAZA_SMS_HELPER_PREFERENCES";

    private const val LAST_APP_HASH = "LAST_APP_HASH"
    private const val FORMATTED_SMS_AND_USSD_LIST = "FORMATTED_SMS_AND_USSD_LIST"
    private const val PHONE_NUMBER = "PHONE_NUMBER"
    private const val TEMP_QUEUE_ELEMENT = "TEMP_QUEUE_ELEMENT"
    private const val QUEUE_LIST = "QUEUE_LIST"
    private const val LAST_TRANSFER_ATTEMPT = "LAST_TRANSFER_ATTEMPT"

    fun getListViewElementList(): MutableList<ListViewElement> {
        val stringsSet = prefs().getStringSet(FORMATTED_SMS_AND_USSD_LIST, setOf())
        val elementsList = mutableListOf<ListViewElement>()
        stringsSet?.forEach {
            try {
                elementsList.add(Gson().fromJson(it, ListViewElement::class.java))
            } catch (e: com.google.gson.JsonSyntaxException) {
            }
        }
        return elementsList
    }

    fun setListViewElementList(list: MutableList<ListViewElement>) {
        val stringsSet = mutableSetOf<String>()
        list.forEach {
            stringsSet.add(Gson().toJson(it))
        }
        editor().putStringSet(FORMATTED_SMS_AND_USSD_LIST, stringsSet).apply()
    }

    fun saveSentToServerElement(element: ListViewElement) {
        val currentList = getListViewElementList()
        currentList.find { it == element }?.isSentToServer = element.isSentToServer
        setListViewElementList(currentList)
    }

    fun addNewListViewElement(element: ListViewElement) {
        val currentList = getListViewElementList()
        if (!currentList.contains(element)) {
            currentList.add(element)
            setListViewElementList(currentList)
        }
    }

    fun getLastAppHash(): String {
        return prefs().getString(LAST_APP_HASH, "") ?: ""
    }

    fun setLastAppHash(hash: String) {
        editor().putString(LAST_APP_HASH, hash).apply()
    }

    fun getPhoneNumber(): String {
        return prefs().getString(PHONE_NUMBER, "") ?: ""
    }

    fun setPhoneNumber(phone: String) {
        editor().putString(PHONE_NUMBER, phone).commit()
    }

    fun getTempQueueElement(): QueueElement? {
        val elem = prefs().getString(TEMP_QUEUE_ELEMENT, null)
        if (elem != null) {
            try {
                return Gson().fromJson(elem, QueueElement::class.java)
            } catch (e: com.google.gson.JsonSyntaxException) {
            }
        }
        return null
    }

    fun setTempQueueElement(queueElement: QueueElement?) {
        editor().putString(TEMP_QUEUE_ELEMENT, Gson().toJson(queueElement)).apply()
    }

    fun getQueueList(): MutableList<QueueElement> {
        val stringsSet = prefs().getStringSet(QUEUE_LIST, setOf())
        val elementsList = mutableListOf<QueueElement>()
        stringsSet?.forEach {
            try {
                elementsList.add(Gson().fromJson(it, QueueElement::class.java))
            } catch (e: com.google.gson.JsonSyntaxException) {
            }
        }
        return elementsList.sortedBy { it.queueId }.toMutableList()
    }

    fun setQueueList(queueList: List<QueueElement>) {
        val stringsSet = mutableSetOf<String>()
        queueList.sortedBy { it.queueId }.forEach {
            stringsSet.add(Gson().toJson(it))
        }
        editor().putStringSet(QUEUE_LIST, stringsSet).apply()
    }

    fun addElementToQueueList(element: QueueElement) {
        val list = getQueueList()
        if (!list.contains(element)) {
            list.add(element)
        }
        setQueueList(list)
    }

    fun removeElementFromQueueList(element: QueueElement) {
        val list = getQueueList()
        if (list.contains(element)) {
            list.remove(element)
        }
        setQueueList(list)
    }

    fun getLastTransferAttempt(): LastTransferAttempt? {
        return try {
            Gson().fromJson(
                prefs().getString(LAST_TRANSFER_ATTEMPT, null),
                LastTransferAttempt::class.java
            )
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun setLastTransferAttempt(attempt: LastTransferAttempt) {
        editor().putString(LAST_TRANSFER_ATTEMPT, Gson().toJson(attempt)).apply()
    }


    private fun prefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun editor(): SharedPreferences.Editor {
        return prefs().edit()
    }
}