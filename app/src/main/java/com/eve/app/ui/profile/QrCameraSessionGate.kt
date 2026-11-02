package com.eve.app.ui.profile

/** EVE CameraX ownership adapter. A stopped session can never bind or deliver a decoded frame later. */
internal class QrCameraSessionGate {
    private var resumed = false
    private var closed = false
    private var generation = 0L
    private var pending: Long? = null
    private var active: Long? = null

    @Synchronized fun resume() { if (!closed) resumed = true }
    @Synchronized fun isResumed(): Boolean = resumed && !closed

    @Synchronized fun beginBind(): Long? {
        if (!resumed || closed || pending != null || active != null) return null
        return (++generation).also { pending = it }
    }

    @Synchronized fun isBinding(ticket: Long): Boolean = resumed && !closed && pending == ticket

    @Synchronized fun completeBind(ticket: Long, success: Boolean): Boolean {
        if (!isBinding(ticket)) return false
        pending = null
        active = if (success) ticket else null
        return true
    }

    @Synchronized fun acceptsFrame(ticket: Long): Boolean = resumed && !closed && active == ticket

    @Synchronized fun pause() {
        resumed = false
        generation++
        pending = null
        active = null
    }

    @Synchronized fun close() { pause(); closed = true }
}
