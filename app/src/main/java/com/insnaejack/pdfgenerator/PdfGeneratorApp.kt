package com.insnaejack.pdfgenerator // Make sure this matches your applicationId or namespace

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PdfGeneratorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this) { initializationStatus ->
            // You can log the status or handle it if needed
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                android.util.Log.d("PdfGeneratorApp", String.format(
                    "Adapter name: %s, Description: %s, Latency: %d",
                    adapterClass, status?.description, status?.latency))
            }
        }
        // Other initialization code can go here if needed
    }
}