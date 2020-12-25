package com.enazamusic.smsapp.services

import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

/*
 * This receiver helps to get USSD (and any outgoing call) requests
 * It is used after SDK 29, since Manifest.permission.PROCESS_OUTGOING_CALLS is deprecated in API level 29
 */
@RequiresApi(Build.VERSION_CODES.Q)
class UssdRequestService: CallRedirectionService() {
    override fun onPlaceCall(handle: Uri, initialPhoneAccount: PhoneAccountHandle, allowInteractiveResponse: Boolean) {
        Toast.makeText(this, "Will be implemented soon", Toast.LENGTH_SHORT).show()
    }
}