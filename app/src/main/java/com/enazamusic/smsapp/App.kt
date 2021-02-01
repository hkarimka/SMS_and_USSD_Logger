package com.enazamusic.smsapp

import android.app.Application
import com.enazamusic.smsapp.viewmodels.ViewModelLoggedDataFragment
import com.enazamusic.smsapp.viewmodels.ViewModelQueueFragment
import org.koin.android.ext.android.startKoin
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

class App : Application() {
    private val module = module {
        viewModel { ViewModelLoggedDataFragment() }
        viewModel { ViewModelQueueFragment() }
        single { }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin(this, listOf(module))
    }
}