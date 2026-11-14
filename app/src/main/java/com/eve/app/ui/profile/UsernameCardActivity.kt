package com.eve.app.ui.profile

import android.content.Intent

/**
 * UsernameCardActivity handles public EVE username QR viewing and live scanner launching.
 * Inherits full authentic Telegram QrShareActivity implementation.
 */
open class UsernameCardActivity : QrShareActivity() {

    fun launchLiveScanner(from: FloatArray) {
        startActivity(
            Intent(this, EveQrLiveScannerActivity::class.java)
                .putExtra(QrScanMorphLayout.EXTRA_ORIGIN, from)
        )
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        fun parsePayload(dataString: String): String? = ProfileQrPayload.parse(dataString)
    }
}
