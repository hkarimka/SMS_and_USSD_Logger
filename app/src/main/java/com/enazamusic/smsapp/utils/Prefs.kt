package com.enazamusic.smsapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.enazamusic.smsapp.model.ListViewElement
import com.google.gson.Gson
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

object Prefs : KoinComponent {
    private val context: Context by inject()
    private const val PREFS_NAME = "ENAZA_SMS_HELPER_PREFERENCES";

    private const val FORMATTED_SMS_AND_USSD_LIST = "FORMATTED_SMS_AND_USSD_LIST"
    private const val LAST_SMS_LIST_RECEIVED_DATE = "LAST_SMS_LIST_RECEIVED_DATE"

    fun getFormattedSmsAndUssdList(): MutableList<ListViewElement> {
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

    fun setFormattedSmsAndUssdList(list: MutableList<ListViewElement>) {
        val stringsSet = mutableSetOf<String>()
        list.forEach {
            stringsSet.add(Gson().toJson(it))
        }
        editor().putStringSet(FORMATTED_SMS_AND_USSD_LIST, stringsSet).apply()
    }

    private fun prefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun editor(): SharedPreferences.Editor {
        return prefs().edit()
    }
}