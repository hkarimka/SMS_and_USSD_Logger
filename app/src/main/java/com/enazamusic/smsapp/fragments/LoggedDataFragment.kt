package com.enazamusic.smsapp.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.enazamusic.smsapp.R
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.viewmodels.ViewModelLoggedDataFragment
import kotlinx.android.synthetic.main.fragment_logged_data.*
import kotlinx.android.synthetic.main.view_non_delivered.view.*
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
                var list = mutableListOf<String>()
                when (intent.action) {
                    BroadcastHelper.ACTION_NEW_SMS_RECEIVED -> {
                        val element =
                            intent.getSerializableExtra(BroadcastHelper.EXTRA_ELEMENT) as ListViewElement?
                                ?: return
                        list = vm.newSmsReceived(element)
                    }
                    BroadcastHelper.ACTION_NEW_USSD_RECEIVED -> {
                        val element =
                            intent.getSerializableExtra(BroadcastHelper.EXTRA_ELEMENT) as ListViewElement?
                                ?: return
                        list = vm.newUssdReceived(element)
                    }
                }
                updateAdapterElements(list)
                checkAndShowNonDeliveredMessagesView()
            }
        }

        permissionsGrantedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateAdapterElements(vm.getFormattedSmsAndUssdList())
            }
        }

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            vm.getFormattedSmsAndUssdList()
        )
        val list = vm.getFormattedSmsAndUssdList()
        adapter =
            object :
                ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, list) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    val tv = v.findViewById<View>(android.R.id.text1) as TextView
                    tv.text = HtmlCompat.fromHtml(list[position], HtmlCompat.FROM_HTML_MODE_LEGACY)
                    return v
                }
            }
        listview.adapter = adapter

        vm.triedToSendLiveData.observe(viewLifecycleOwner, {
            if (it) {
                checkAndShowNonDeliveredMessagesView()
                updateAdapterElements(vm.getFormattedSmsAndUssdList())
            }
        })
        registerReceivers()
        updateAdapterElements(vm.getFormattedSmsAndUssdList())
        checkAndShowNonDeliveredMessagesView()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    private fun updateAdapterElements(list: MutableList<String>) {
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
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(permissionsGrantedReceiver)
    }

    private fun checkAndShowNonDeliveredMessagesView() {
        val count = vm.getNonDeliveredMessages().size
        val date = vm.getLastTransferAttemptDate()
        val code = vm.getLastTransferAttemptCode()
        if (count > 0 && date.isNotBlank() && code != 0) {
            val text = getString(R.string.non_delivered_messages, count, date, code)
            non_delivered.textview.text = text
            non_delivered.button.setOnClickListener {
                vm.tryToSendElements()
                non_delivered.visibility = View.GONE
            }
            non_delivered.visibility = View.VISIBLE
        } else {
            non_delivered.visibility = View.GONE
        }
    }
}