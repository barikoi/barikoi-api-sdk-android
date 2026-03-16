package com.barikoi.barikoiapis

import android.app.Application
import com.barikoi.sdk.BarikoiClient

/**
 * Initialises the Barikoi SDK once for the whole app.
 *
 * Call [BarikoiClient.init] here so the API key and logging flag are
 * configured before any Activity or Fragment tries to create a client.
 *
 * In every Activity / Fragment, just write:
 * ```kotlin
 * private val barikoi = BarikoiClient()
 * ```
 */
class BarikoiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        BarikoiClient.init(
            apiKey        = BuildConfig.BARIKOI_API_KEY,
            enableLogging = BuildConfig.DEBUG
        )
    }
}
