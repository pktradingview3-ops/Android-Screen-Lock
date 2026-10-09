package com.timewall.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks up the context chain to the hosting Activity, or null if there is none. */
fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
