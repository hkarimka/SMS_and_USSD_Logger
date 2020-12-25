package com.enazamusic.smsapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.BroadcastHelper
import java.util.*

/*
 * This receiver helps to get USSD (and any outgoing call) requests
 * It is used before SDK 29, since Manifest.permission.PROCESS_OUTGOING_CALLS is deprecated in API level 29
 */
class UssdRequestReceiver : BroadcastReceiver() {

    private val OUTGOING_CALL_ACTION = "android.intent.action.NEW_OUTGOING_CALL"
    private val INTENT_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == OUTGOING_CALL_ACTION) {
            val phoneNumber = intent.extras?.getString(INTENT_PHONE_NUMBER) ?: ""
            if (phoneNumber.startsWith("*") && phoneNumber.endsWith("#")) {
                val ussd = ListViewElement(
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(),
                    false,
                    ListViewElement.Direction.OUT,
                    ListViewElement.Type.USSD,
                    null,
                    phoneNumber
                )
                BroadcastHelper.newUssdReceived(ussd)
            }
        }
    }
}