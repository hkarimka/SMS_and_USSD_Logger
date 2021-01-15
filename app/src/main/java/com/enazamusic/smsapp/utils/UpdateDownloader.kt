package com.enazamusic.smsapp.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import com.enazamusic.smsapp.R
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.io.File


object UpdateDownloader : KoinComponent {

    private val context: Context by inject()

    fun checkAndInstallUpdate() {
    }

    fun checkAndInstallUpdateStub() {
        var destination: String =
            context.getExternalFilesDir(null)
                .toString() + "/"
        val fileName = "AppName.apk"
        destination += fileName
        //Delete update file if exists
        val file = File(destination)
        if (file.exists()) {
            file.delete()
        }

        //get url of app on server
        val url = "http://hunterxp.ru/files/EnazaMusicLogger_v.1.0.apk"

        //set downloadmanager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setDescription(context.getString(R.string.download_description))
        request.setTitle(context.getString(R.string.app_name))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // get download service and enqueue file
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        //set BroadcastReceiver to install app when .apk is downloaded
        val onComplete: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (isDownloadSuccess(downloadId)) {
                    val uri = manager.getUriForDownloadedFile(downloadId)
                    val install = Intent(Intent.ACTION_VIEW)
                    install.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    install.setDataAndType(
                        uri,
                        "application/vnd.android.package-archive"
                    )
                    context.startActivity(install)
                }
                context.unregisterReceiver(this)
            }
        }
        //register receiver for when .apk download is complete
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun isDownloadSuccess(downloadId: Long): Boolean {
        //Verify if download is a success
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val c: Cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
        if (c.moveToFirst()) {
            val status: Int = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                return true //Download is valid
            } else {
                val reason: Int = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON))
            }
        }
        return false
    }
}