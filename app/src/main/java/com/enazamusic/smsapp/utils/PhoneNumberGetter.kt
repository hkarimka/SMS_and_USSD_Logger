package com.enazamusic.smsapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

object PhoneNumberGetter : KoinComponent {
    private val context: Context by inject()
    fun tryToGetPhoneNumber(): String {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val number = telephonyManager.line1Number
                if (!number.isNullOrBlank()) {
                    return number
                }
            } catch (e: Exception) {
            }
        }
        return ""
    }
}