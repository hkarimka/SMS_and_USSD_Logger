package com.enazamusic.smsapp.receivers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.model.QueueElement
import com.enazamusic.smsapp.utils.API
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.utils.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_NAME = "com.enazamusic.smsapp.AlarmReceived"
        private var isRunning = false
        private val tasksQueue: Queue<QueueElement> = LinkedList()
    }

    private val timer = Timer()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val bgDispatcher = Dispatchers.IO
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_NAME) {
            uiScope.launch {
                val apiList: List<QueueElement>
                withContext(bgDispatcher) {
                    //list = API.checkNewQueueMessages()
                    apiList = Prefs.getQueueList()
                }
                val prefsList = Prefs.getQueueList()
                val currentList = prefsList.union(apiList).toList()
                currentList.forEach {
                    if (!tasksQueue.contains(it)) {
                        tasksQueue.add(it)
                    }
                }
                Prefs.setQueueList(tasksQueue.toMutableList())
                BroadcastHelper.queueUpdated()
                if (!isRunning) {
                    timer.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            if (tasksQueue.isNotEmpty()) {
                                isRunning = true
                                val elem = tasksQueue.poll()
                                handleElement(context, elem)
                            } else {
                                isRunning = false
                                cancel()
                            }
                        }
                    }, 0, 15000)
                }
            }

        }
    }

    private fun handleElement(context: Context, element: QueueElement?) {
        if (element != null) {
            when (element.type) {
                ListViewElement.Type.SMS -> {
                    sendSms(context, element)
                }
                ListViewElement.Type.USSD -> {
                    makeUssdCall(context, element)
                }
            }
            BroadcastHelper.queueUpdated()
        }
    }

    private fun makeUssdCall(context: Context, element: QueueElement) {
        if (!element.text.startsWith("*") || !element.text.endsWith("#")) return
        val intent = Intent(Intent.ACTION_CALL)
        val ussd = "tel:" + element.text.substring(0, element.text.length - 1) + Uri.encode("#")
        intent.data = Uri.parse(ussd)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            element.isDone = true
            Prefs.removeElementFromQueueList(element)
            Prefs.setTempQueueElement(element)
            context.startActivity(intent)
        }
    }

    private fun sendSms(context: Context, element: QueueElement) {
        if (element.smsDestination.isNullOrBlank() || element.text.isBlank()) return
        val smsManager: SmsManager = SmsManager.getDefault()
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            element.isDone = true
            smsManager.sendTextMessage(element.smsDestination, null, element.text, null, null)
            val listViewElement = ListViewElement(
                element.queueId,
                3000 * (System.currentTimeMillis() / 3000),
                ListViewElement.Direction.OUT,
                ListViewElement.Type.SMS,
                element.smsDestination,
                element.text
            )
            Prefs.removeElementFromQueueList(element)
            BroadcastHelper.newSmsReceived(listViewElement)
            Prefs.addNewListViewElement(listViewElement)
            API.logUserMessage(listViewElement)
        }
    }
}