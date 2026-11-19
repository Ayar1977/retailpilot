package com.salespro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.salespro.workers.StockReminderWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class RetailPilotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.salespro.settings.AppSettings.init(this)

        // Stock Reminders are now scheduled precisely per-event rather than polling.
    }
}
