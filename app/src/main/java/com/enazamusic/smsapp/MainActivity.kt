package com.enazamusic.smsapp

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.enazamusic.smsapp.adapters.ViewPagerAdapter
import com.enazamusic.smsapp.fragments.LoggedDataFragment
import com.enazamusic.smsapp.fragments.QueueFragment
import com.enazamusic.smsapp.model.ViewPagerElement
import com.enazamusic.smsapp.receivers.SmsReceiver
import com.enazamusic.smsapp.services.UssdResponseService
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.viewmodels.ViewModelMainActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent

class MainActivity : AppCompatActivity(), KoinComponent {

    private val vm: ViewModelMainActivity by viewModel()
    private val requestPermissionsCode = 123
    private var enableAccessibilityIntentStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setupViewPager()
        checkAndAskPermissions()
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
        }
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
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.PROCESS_OUTGOING_CALLS)
            subtitle = getString(
                R.string.grant_permissions_subtitle,
                getString(R.string.grant_permission_subt_pre30_addition)
            )
        } else {
            permissions = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
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
                    requestPermissionsCode
                )
            }
            builder.show()
        } else {
            BroadcastHelper.permissionsGranted()
            checkAndStartAccessibilityService()
        }
    }

    private fun checkRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
            startActivityForResult(intent, 0)
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

    private fun checkAndStartAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.enable_accessibility_title))
            builder.setMessage(getString(R.string.enable_accessibility_subtitle))
            builder.setOnCancelListener {
                if (!isAccessibilityServiceEnabled()) {
                    showServiceNecessarySnackbar()
                }
            }
            builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
                enableAccessibilityIntentStarted = true
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            builder.show()
        }
    }

    private fun showServiceNecessarySnackbar() {
        Snackbar.make(toolbar, R.string.enable_accessibility_service, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            requestPermissionsCode -> {
                var permissionsGranted = false
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsGranted = true
                    }
                } else {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        permissionsGranted = true
                    }
                }
                if (permissionsGranted) {
                    BroadcastHelper.permissionsGranted()
                    checkAndStartAccessibilityService()
                } else {
                    showPermissionsNecessarySnackbar()
                }
            }
        }
    }
}