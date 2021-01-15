package com.enazamusic.smsapp.viewmodels

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.Prefs
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.*

class ViewModelLoggedDataFragment : ViewModel(), KoinComponent {
    private val context: Context by inject()
    private var formattedSmsAndUssdList = mutableListOf<ListViewElement>()
    private var ussdList = mutableListOf<ListViewElement>()

    fun newSmsReceived(): MutableList<String> {
        val list = getMergedLists()
        Prefs.setFormattedSmsAndUssdList(list)
        return formatList(list)
    }

    fun newUssdReceived(ussd: ListViewElement): MutableList<String> {
        if (!ussdList.contains(ussd) && !formattedSmsAndUssdList.contains(ussd)) {
            ussdList.add(ussd)
        }
        val list = getMergedLists()
        Prefs.setFormattedSmsAndUssdList(list)
        return formatList(list)
    }

    fun getFormattedSmsAndUssdList(): MutableList<String> {
        return formatList(getMergedLists())
    }

    private fun formatList(list: MutableList<ListViewElement>): MutableList<String> {
        val formattedList = mutableListOf<String>()
        list.forEach {
            formattedList.add(it.formatted())
        }
        return formattedList
    }

    private fun getMergedLists(): MutableList<ListViewElement> {
        val prefsList = Prefs.getFormattedSmsAndUssdList()
        val smsList = getAllSms()
        var uniqueSet = prefsList.union(smsList)
        uniqueSet = uniqueSet.union(ussdList)
        val uniqueList = uniqueSet.sortedBy { it.date }.toMutableList()
        ussdList.clear()
        formattedSmsAndUssdList.clear()
        formattedSmsAndUssdList.addAll(uniqueList)
        return formattedSmsAndUssdList
    }

    private fun getAllSms(): MutableList<ListViewElement> {
        val lstSms = mutableListOf<ListViewElement>()
        if (!readSmsPermissionGranted()) return lstSms
        val message: Uri = Uri.parse("content://sms/")
        val cr: ContentResolver = context.applicationContext.contentResolver
        val c: Cursor? = cr.query(message, null, null, null, null)
        val totalSMS = c?.count ?: 0
        val lastTime =
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        if (c?.moveToFirst() == true) {
            for (i in 0 until totalSMS) {
                val date = c.getString(c.getColumnIndexOrThrow("date")).toLong()
                if (date >= lastTime) {
                    val address = c.getString(c.getColumnIndexOrThrow("address"))
                    val msg = c.getString(c.getColumnIndexOrThrow("body"))
                    val type = if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                        ListViewElement.Direction.IN
                    } else {
                        ListViewElement.Direction.OUT
                    }
                    val obj = ListViewElement(
                        UUID.randomUUID().toString(), date, false, type,
                        ListViewElement.Type.SMS, address, msg
                    )
                    lstSms.add(obj)
                }
                c.moveToNext()
            }
        }
        c?.close()
        return lstSms
    }

    private fun readSmsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}