package com.cyryel.ui.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.cyryel.R

fun Context.showToast(message: String) {
    val layout = LayoutInflater.from(this).inflate(R.layout.toast_copied, null)
    layout.findViewById<android.widget.TextView>(R.id.toastText).text = message
    val density = resources.displayMetrics.density
    Toast(this).apply {
        duration = Toast.LENGTH_SHORT
        view = layout
        setGravity(Gravity.BOTTOM, 0, (50 * density).toInt())
        show()
    }
}
