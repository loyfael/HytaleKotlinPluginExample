package com.loyfael.exampleplugin.listeners

object Log {
    // Disabled by default; change to true for local debug only
    var DEBUG: Boolean = false

    fun dbg(msg: String) {
        if (DEBUG) println("[StaffChat] $msg")
    }
}
