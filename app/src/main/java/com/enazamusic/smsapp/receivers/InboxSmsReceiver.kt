package com.enazamusic.smsapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.API
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.utils.Prefs

/*
 * This receiver helps to get inbox SMS message and notify about that LoggedDataFragment
 */

class InboxSmsReceiver : BroadcastReceiver() {

    private val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SMS_RECEIVED) {
            val mess = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (mess.isNotEmpty()) {
                val address = mess[0].displayOriginatingAddress
                var text = ""
                var date = 0L
                mess.iterator().forEach {
                    if (it.displayOriginatingAddress == address) {
                        text += it.displayMessageBody
                        date = 3000 * (it.timestampMillis / 3000)
                    }
                }

                val element = ListViewElement(
                    null, date, ListViewElement.Direction.IN, ListViewElement.Type.SMS,
                    address, text
                )
                BroadcastHelper.newSmsReceived(element)
                Prefs.addNewListViewElement(element)
                API.logUserMessage(element)
            }
        }
    }
}