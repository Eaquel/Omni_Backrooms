package com.omni.backrooms

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("omni_backrooms")
    }
}

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("omni_backrooms")
        appScope.launch { runInitialGuardCheck() }
    }

    private fun runInitialGuardCheck() {
        val bridge = Native_Bridge()
        val sigHash = BuildConfig.EXPECTED_SIG_HASH
        bridge.initGuard(applicationContext, sigHash)
        val flags = bridge.getGuardFlags()
        if (flags != 0) {
            android.util.Log.w("OmniApp", "Guard flags: ${bridge.getThreatReport()}")
        }
    }

    companion object {
        fun get(ctx: Context): App = ctx.applicationContext as App
    }
}
