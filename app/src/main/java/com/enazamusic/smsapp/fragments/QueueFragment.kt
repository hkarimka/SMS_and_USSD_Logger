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
import com.enazamusic.smsapp.utils.BroadcastHelper
import com.enazamusic.smsapp.viewmodels.ViewModelQueueFragment
import kotlinx.android.synthetic.main.fragment_logged_data.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent

class QueueFragment : Fragment(), KoinComponent {
    companion object {
        fun newInstance(): QueueFragment {
            val args = Bundle()
            val fragment = QueueFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val vm: ViewModelQueueFragment by viewModel()
    private lateinit var queueReceiver: BroadcastReceiver
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        queueReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BroadcastHelper.ACTION_QUEUE_UPDATED) {
                    val list = vm.getFormattedQueueList()
                    updateAdapterElements(list)
                }
            }
        }

        val list = vm.getFormattedQueueList()
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
        registerReceivers()
        updateAdapterElements(vm.getFormattedQueueList())
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
        val queueReceiverFilter = IntentFilter()
        queueReceiverFilter.addAction(BroadcastHelper.ACTION_QUEUE_UPDATED)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(queueReceiver, queueReceiverFilter)
    }

    private fun unregisterReceivers() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(queueReceiver)
    }
}