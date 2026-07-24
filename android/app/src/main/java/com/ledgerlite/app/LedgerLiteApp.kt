package com.ledgerlite.app

import android.app.Application
import com.ledgerlite.app.di.AppContainer

class LedgerLiteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
