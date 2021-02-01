package com.enazamusic.smsapp.viewmodels

import androidx.lifecycle.ViewModel
import com.enazamusic.smsapp.utils.Prefs

class ViewModelQueueFragment : ViewModel() {

    fun getFormattedQueueList(): MutableList<String> {
        val stringList = mutableListOf<String>()
        val list = Prefs.getQueueList()
        list.forEach {
            stringList.add(it.formatted())
        }
        return stringList
    }
}