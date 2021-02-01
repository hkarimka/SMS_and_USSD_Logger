package com.enazamusic.smsapp

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.enazamusic.smsapp.adapters.ViewPagerAdapter
import com.enazamusic.smsapp.fragments.LoggedDataFragment
import com.enazamusic.smsapp.fragments.QueueFragment
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.model.QueueElement
import com.enazamusic.smsapp.model.ViewPagerElement
import com.enazamusic.smsapp.receivers.AlarmReceiver
import com.enazamusic.smsapp.services.UssdResponseService
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.utils.PhoneNumberGetter
import com.enazamusic.smsapp.utils.Prefs
import com.enazamusic.smsapp.utils.UpdateDownloader
import com.enazamusic.smsapp.viewmodels.ViewModelMainActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_queue_element.view.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent

class MainActivity : AppCompatActivity(), KoinComponent {

    private val vm: ViewModelMainActivity by viewModel()
    private val requestCodePermissionsCode = 123
    private var enableAccessibilityIntentStarted = false
    private var allowAppInstallingIntentStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setupViewPager()
        checkAndAskPermissions()
        UpdateDownloader.isUpdateAvailableLiveData.observe(this, {
            if (it) {
                Snackbar.make(toolbar, R.string.update_is_available, Snackbar.LENGTH_SHORT).show()
                if (isAppInstallingAllowed()) {
                    UpdateDownloader.downloadAndInstallUpdate()
                } else {
                    showAppInstallingNecessarySnackbar()
                }
            } else {
                Snackbar.make(toolbar, R.string.update_is_not_available, Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
        startAlarmManager()
    }

    private fun startAlarmManager() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmIntent.action = AlarmReceiver.ACTION_NAME
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime(),
            60000,
            pendingIntent
        )
    }

    override fun onResume() {
        super.onResume()
        if (enableAccessibilityIntentStarted) {
            if (isAccessibilityServiceEnabled()) {
                Snackbar.make(toolbar, R.string.service_is_enabled, Snackbar.LENGTH_SHORT).show()
            } else {
                showServiceNecessarySnackbar()
            }
            enableAccessibilityIntentStarted = false
            checkAndAllowAppInstalling()
        }
        if (allowAppInstallingIntentStarted) {
            if (isAppInstallingAllowed()) {
                Snackbar.make(toolbar, R.string.app_installing_is_allowed, Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                showAppInstallingNecessarySnackbar()
            }
            allowAppInstallingIntentStarted = false
            checkAndAskPhoneNumber()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val item = menu.findItem(R.id.menu_check_for_updates)
        item.title = getString(R.string.toolbar_menu_check_for_updates, BuildConfig.VERSION_NAME)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_check_for_updates -> {
                Snackbar.make(toolbar, R.string.checking_for_update, Snackbar.LENGTH_LONG).show()
                UpdateDownloader.isUpdateAvailable()
                return true
            }
            R.id.menu_about -> {
                Toast.makeText(this, "about", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.change_phone_number -> {
                showEnterPhoneNumberDialog()
                return true
            }
            R.id.add_queue_elem -> {
                addQueueElementDialog()
                return true
            }
        }
        return false
    }

    private fun setupViewPager() {
        val elementsList = mutableListOf<ViewPagerElement>()
        elementsList.add(
            ViewPagerElement(
                getString(R.string.viewpager_title_logged_data),
                LoggedDataFragment.newInstance()
            )
        )
        elementsList.add(
            ViewPagerElement(
                getString(R.string.viewpager_title_queue),
                QueueFragment.newInstance()
            )
        )
        viewpager.adapter = ViewPagerAdapter(supportFragmentManager, elementsList)
        tablayout.setupWithViewPager(viewpager)
    }

    private fun checkAndAskPermissions() {
        val subtitle: String
        val permissions: Array<String>
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissions =
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.PROCESS_OUTGOING_CALLS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE
                )
            subtitle = getString(
                R.string.grant_permissions_subtitle,
                getString(R.string.grant_permission_subt_pre30_addition)
            )
        } else {
            permissions = arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
            )
            subtitle = getString(R.string.grant_permissions_subtitle, "")
        }
        var allPermissionsGranted = true
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                allPermissionsGranted = false
            }
        }
        if (!allPermissionsGranted) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.grant_permissions_title))
            builder.setMessage(subtitle)
            builder.setOnCancelListener {
                showPermissionsNecessarySnackbar()
            }
            builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    requestCodePermissionsCode
                )
            }
            builder.show()
        } else {
            BroadcastHelper.permissionsGranted()
            checkAndStartAccessibilityService()
        }
    }

    private fun showPermissionsNecessarySnackbar() {
        Snackbar.make(toolbar, R.string.permission_necessary, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName == packageName && enabledServiceInfo.name == UssdResponseService::class.java.name) {
                return true
            }
        }
        return false
    }

    private fun isAppInstallingAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else true
    }

    private fun checkAndStartAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.enable_accessibility_title))
            builder.setMessage(getString(R.string.enable_accessibility_subtitle))
            builder.setOnCancelListener {
                if (!isAccessibilityServiceEnabled()) {
                    showServiceNecessarySnackbar()
                    checkAndAllowAppInstalling()
                }
            }
            builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
                enableAccessibilityIntentStarted = true
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            builder.show()
        } else {
            checkAndAllowAppInstalling()
        }
    }

    private fun checkAndAllowAppInstalling() {
        if (!isAppInstallingAllowed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.allow_app_installing_title))
            builder.setMessage(getString(R.string.allow_app_installing_subtitle))
            builder.setOnCancelListener {
                if (!isAppInstallingAllowed()) {
                    showAppInstallingNecessarySnackbar()
                    checkAndAskPhoneNumber()
                }
            }
            builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
                allowAppInstallingIntentStarted = true
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            builder.show()
        } else {
            checkAndAskPhoneNumber()
        }
    }

    private fun checkAndAskPhoneNumber() {
        if (Prefs.getPhoneNumber().isBlank()) {
            val guessedPhone = PhoneNumberGetter.tryToGetPhoneNumber()
            if (guessedPhone.isNotBlank()) {
                Snackbar.make(
                    toolbar,
                    getString(R.string.confirm_phone_number, Prefs.getPhoneNumber()),
                    7000
                )
                    .setAction(R.string.change) {
                        showEnterPhoneNumberDialog()
                    }
                    .show()
            } else {
                showEnterPhoneNumberDialog()
            }
        }
    }

    private fun showEnterPhoneNumberDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.please_enter_number_title)
        builder.setMessage(R.string.please_enter_number_subtitle)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_PHONE
        input.gravity = Gravity.CENTER
        input.textSize = 18f
        builder.setView(input)
        builder.setOnCancelListener {
            showCurrentPhoneNumberSnackbar()
        }
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            val phone = input.text.toString()
            if (phone.isNotBlank()) {
                Prefs.setPhoneNumber(phone)
            }
            showCurrentPhoneNumberSnackbar()
        }
        builder.show()
        input.requestFocus()
    }

    private fun showServiceNecessarySnackbar() {
        Snackbar.make(toolbar, R.string.enable_accessibility_service, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun showAppInstallingNecessarySnackbar() {
        Snackbar.make(toolbar, R.string.allow_app_installing_snackbar, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun showCurrentPhoneNumberSnackbar() {
        Snackbar.make(
            toolbar,
            getString(R.string.current_phone_number, Prefs.getPhoneNumber()),
            Snackbar.LENGTH_LONG
        )
            .show()
    }

    private fun addQueueElementDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_queue_element, null)
        val builder = AlertDialog.Builder(this)
        dialogView.spType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (dialogView.spType.selectedItem.toString() == "USSD") {
                    dialogView.tvDestination.visibility = View.GONE
                    dialogView.etDestination.visibility = View.GONE
                    dialogView.etText.hint = "*100#"
                } else {
                    dialogView.tvDestination.visibility = View.VISIBLE
                    dialogView.etDestination.visibility = View.VISIBLE
                    dialogView.etText.hint = "Hello"
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        builder.setView(dialogView)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            val queueId = dialogView.etQueueId.text.toString()
            val type = ListViewElement.Type.valueOf(dialogView.spType.selectedItem.toString())
            val destination = if (dialogView.etDestination.text.toString().isNotBlank()) {
                dialogView.etDestination.text.toString()
            } else null
            val text = dialogView.etText.text.toString()
            if (queueId.isNotBlank() && text.isNotBlank()) {
                val elem = QueueElement(queueId, type, destination, text)
                Prefs.addElementToQueueList(elem)
                BroadcastHelper.queueUpdated()
                Snackbar.make(toolbar, R.string.task_planned, Snackbar.LENGTH_LONG).show()
                viewpager.setCurrentItem(1, true)
            } else {
                Snackbar.make(toolbar, R.string.something_wrong, Snackbar.LENGTH_LONG).show()
            }
        }
        builder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            requestCodePermissionsCode -> {
                var permissionsGranted = true
                grantResults.indices.forEach { i ->
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        permissionsGranted = false
                    }
                }
                if (permissionsGranted) {
                    BroadcastHelper.permissionsGranted()
                } else {
                    showPermissionsNecessarySnackbar()
                }
                checkAndStartAccessibilityService()
            }
        }
    }
}