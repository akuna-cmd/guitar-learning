package com.guitarlearning.presentation.ui

import android.content.Context
import android.webkit.WebView

object WebViewWarmup {
    @Volatile
    private var warmed = false

    fun warm(context: Context) {
        if (warmed) return
        synchronized(this) {
            if (warmed) return
            runCatching {
                val webView = WebView(context.applicationContext)
                webView.loadUrl("about:blank")
                webView.destroy()
            }
            warmed = true
        }
    }
}
