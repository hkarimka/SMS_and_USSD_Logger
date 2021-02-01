package com.enazamusic.smsapp.viewmodels

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.API
import com.enazamusic.smsapp.utils.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.text.SimpleDateFormat

class ViewModelLoggedDataFragment : ViewModel(), KoinComponent {
    private val context: Context by inject()
    private var smsAndUssdList = mutableListOf<ListViewElement>()
    private var tempList = mutableListOf<ListViewElement>()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    val triedToSendLiveData = MutableLiveData<Boolean>()

    fun newSmsReceived(sms: ListViewElement): MutableList<String> {
        if (!tempList.contains(sms) && !smsAndUssdList.contains(sms)) {
            tempList.add(sms)
        }
        val list = getMergedLists()
        Prefs.setListViewElementList(list)
        return formatList(list)
    }

    fun newUssdReceived(ussd: ListViewElement): MutableList<String> {
        if (!tempList.contains(ussd) && !smsAndUssdList.contains(ussd)) {
            tempList.add(ussd)
        }
        val list = getMergedLists()
        Prefs.setListViewElementList(list)
        return formatList(list)
    }

    fun getFormattedSmsAndUssdList(): MutableList<String> {
        return formatList(getMergedLists())
    }

    fun tryToSendElements() {
        uiScope.launch {
            API.logUserMessages(getNonDeliveredMessages())
            triedToSendLiveData.postValue(true)
        }
    }

    private fun formatList(list: MutableList<ListViewElement>): MutableList<String> {
        val formattedList = mutableListOf<String>()
        list.forEach {
            formattedList.add(it.formatted())
        }
        return formattedList
    }

    private fun getMergedLists(): MutableList<ListViewElement> {
        val prefsList = Prefs.getListViewElementList()
        //val smsList = getAllSms()
        var uniqueSet = prefsList.union(tempList)
        //uniqueSet = uniqueSet.union(smsList)
        val uniqueList = uniqueSet.sortedBy { it.date }.toMutableList()
        tempList.clear()
        smsAndUssdList.clear()
        smsAndUssdList.addAll(uniqueList)
        return smsAndUssdList
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
                val date = 3000 * (c.getString(c.getColumnIndexOrThrow("date")).toLong() / 3000)
                if (date >= lastTime) {
                    val address = c.getString(c.getColumnIndexOrThrow("address"))
                    val msg = c.getString(c.getColumnIndexOrThrow("body"))
                    val type = if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                        ListViewElement.Direction.IN
                    } else {
                        ListViewElement.Direction.OUT
                    }
                    val obj =
                        ListViewElement(null, date, type, ListViewElement.Type.SMS, address, msg)
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

    fun getNonDeliveredMessages(): List<ListViewElement> {
        return getMergedLists().filter { !it.isSentToServer }
    }

    fun getLastTransferAttemptDate(): String {
        val attempt = Prefs.getLastTransferAttempt()
        return if (attempt != null && attempt.date != 0L) {
            SimpleDateFormat("y-MM-d HH:mm:ss").format(attempt.date)
        } else {
            ""
        }
    }

    fun getLastTransferAttemptCode(): Int {
        val attempt = Prefs.getLastTransferAttempt()
        return if (attempt != null && attempt.httpResponseCode != 0) {
            attempt.httpResponseCode
        } else {
            0
        }
    }
}