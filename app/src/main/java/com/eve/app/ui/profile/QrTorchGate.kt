package com.eve.app.ui.profile

/** EVE CameraX request generation state, no Telegram camera/session dependencies. */
internal class QrTorchGate {
    data class Request(val generation: Long, val enable: Boolean)

    var enabled = false
        private set
    private var generation = 0L
    private var pending: Request? = null

    fun request(): Request? {
        if (pending != null) return null
        return Request(++generation, !enabled).also { pending = it }
    }

    fun complete(request: Request, success: Boolean): Boolean {
        if (pending != request) return false
        pending = null
        if (success) enabled = request.enable
        return true
    }

    fun reset() {
        generation++
        pending = null
        enabled = false
    }
}
