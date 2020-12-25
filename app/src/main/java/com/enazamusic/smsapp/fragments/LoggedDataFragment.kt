package com.enazamusic.smsapp.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.enazamusic.smsapp.R
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.viewmodels.ViewModelLoggedDataFragment
import kotlinx.android.synthetic.main.fragment_logged_data.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent

class LoggedDataFragment : Fragment(), KoinComponent {
    companion object {
        fun newInstance(): LoggedDataFragment {
            val args = Bundle()
            val fragment = LoggedDataFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val vm: ViewModelLoggedDataFragment by viewModel()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var smsAndUssdReceiver: BroadcastReceiver
    private lateinit var permissionsGrantedReceiver: BroadcastReceiver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logged_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        smsAndUssdReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BroadcastHelper.ACTION_NEW_SMS_RECEIVED -> {
                        vm.newSmsReceived()
                    }
                    BroadcastHelper.ACTION_NEW_USSD_RECEIVED -> {
                        val element = intent.getParcelableExtra<ListViewElement>(BroadcastHelper.EXTRA_USSD) ?: return
                        vm.newUssdReceived(element)
                    }
                }
                updateAdapterElements()
            }
        }

        permissionsGrantedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateAdapterElements()
            }
        }

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            vm.getFormattedSmsAndUssdList()
        )
        listview.adapter = adapter
        registerReceivers()
        updateAdapterElements()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    private fun updateAdapterElements() {
        val list = vm.getFormattedSmsAndUssdList()
        if (list.isNotEmpty()) {
            adapter.clear()
            adapter.addAll(list)
            adapter.notifyDataSetChanged()
            tvListIsEmpty.visibility = View.GONE
            listview.visibility = View.VISIBLE
            listview.setSelection(list.size - 1)
        } else {
            tvListIsEmpty.visibility = View.VISIBLE
            listview.visibility = View.GONE
        }
    }

    private fun registerReceivers() {
        val smsAndUssdReceiverFilter = IntentFilter()
        smsAndUssdReceiverFilter.addAction(BroadcastHelper.ACTION_NEW_SMS_RECEIVED)
        smsAndUssdReceiverFilter.addAction(BroadcastHelper.ACTION_NEW_USSD_RECEIVED)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(smsAndUssdReceiver, smsAndUssdReceiverFilter)

        val permissionsGrantedFilter = IntentFilter()
        permissionsGrantedFilter.addAction(BroadcastHelper.ACTION_PERMISSIONS_GRANTED)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(permissionsGrantedReceiver, permissionsGrantedFilter)
    }

    private fun unregisterReceivers() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(smsAndUssdReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(permissionsGrantedReceiver)
    }
}