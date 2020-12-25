package com.enazamusic.smsapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.BroadcastHelper
import org.koin.standalone.KoinComponent
import java.util.*

/*
 * This receiver helps to get inbox SMS events and notify about that LoggedDataFragment
 * to update SMS history and read a new SMS
 */

class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"

    /*
     * It's also possible to get a new SMS data using Telephony.Sms.Intents.getMessagesFromIntent(intent)
     * but onReceive() is called several times with incomplete SMS text so
     * we just send a broadcast here and get a new SMS using ContentResolver reading SMS history
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SMS_RECEIVED) {
            BroadcastHelper.newSmsReceived()
        }
    }
}