package com.enazamusic.smsapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.enazamusic.smsapp.R
import com.enazamusic.smsapp.viewmodels.ViewModelQueueFragment

class QueueFragment: Fragment() {
    companion object {
        fun newInstance(): QueueFragment {
            val args = Bundle()
            val fragment = QueueFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var viewModel: ViewModelQueueFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this).get(ViewModelQueueFragment::class.java)
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}