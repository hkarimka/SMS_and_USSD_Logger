package com.enazamusic.smsapp.utils

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.enazamusic.smsapp.model.ListViewElement
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

object BroadcastHelper : KoinComponent {
    private val context: Context by inject()

    const val ACTION_NEW_SMS_RECEIVED = "ACTION_NEW_SMS_RECEIVED"
    const val ACTION_NEW_USSD_RECEIVED = "ACTION_NEW_USSD_RECEIVED"
    const val EXTRA_USSD = "EXTRA_USSD"
    const val ACTION_PERMISSIONS_GRANTED = "ACTION_PERMISSIONS_GRANTED"

    fun newSmsReceived() {
        val intent = Intent(ACTION_NEW_SMS_RECEIVED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun newUssdReceived(ussd: ListViewElement) {
        val intent = Intent(ACTION_NEW_USSD_RECEIVED)
        intent.putExtra(EXTRA_USSD, ussd)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun permissionsGranted() {
        val intent = Intent(ACTION_PERMISSIONS_GRANTED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}