package com.CYRYEL.com.ui.notifications

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FcmDebugStore {
    private val messages = mutableListOf<FcmLogEntry>()

    fun log(entry: FcmLogEntry) {
        synchronized(messages) {
            messages.add(entry)
            if (messages.size > 100) messages.removeAt(0)
        }
    }

    fun getLogs(): List<FcmLogEntry> = synchronized(messages) { messages.toList() }

    fun clear() {
        synchronized(messages) { messages.clear() }
    }
}

data class FcmLogEntry(
    val type: FcmLogType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String get() {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

enum class FcmLogType {
    TOKEN_GENERATED,
    TOKEN_SAVED,
    TOKEN_SAVE_FAILED,
    MESSAGE_RECEIVED,
    MESSAGE_SHOWN,
    MESSAGE_DROPPED,
    PERMISSION_CHECK,
    ERROR
}
