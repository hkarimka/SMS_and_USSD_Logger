package com.enazamusic.smsapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.API
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.utils.Prefs

/*
 * This service helps to get USSD responses using Android Accessibility Service,
 * since Android SDK doesn't provide any API for interacting with USSD calls
 */

class UssdResponseService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val source = event.source
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            && !event.className.toString().contains("AlertDialog")
        ) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && (source == null || source.className != "android.widget.TextView")
        ) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && source.text.isBlank()
        ) {
            return
        }

        val text = if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.text.toString()
        } else {
            source.text.toString()
        }

        // in some devices, onAccessibilityEvent() function is called multiple times for one response,
        // so we round time to one second to get only unique responses
        val timeRoundedToOneSecond = 1000 * (System.currentTimeMillis() / 1000)
        val queueElement = Prefs.getTempQueueElement()
        val queueId =
            if (queueElement != null && queueElement.type == ListViewElement.Type.USSD) {
                Prefs.setTempQueueElement(null)
                queueElement.queueId
            } else {
                null
            }
        val ussd = ListViewElement(
            queueId, timeRoundedToOneSecond, ListViewElement.Direction.IN,
            ListViewElement.Type.USSD, null, text
        )
        BroadcastHelper.newUssdReceived(ussd)
        Prefs.addNewListViewElement(ussd)
        API.logUserMessage(ussd)
    }


    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.flags = AccessibilityServiceInfo.DEFAULT
        info.packageNames = arrayOf("com.android.phone")
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info
    }
}