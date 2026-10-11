package com.smartmove.ai.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.ui.theme.SmartMoveBackground

/**
 * SMARTMOVE AI — High-Performance Web Google Maps & Navigation Screen.
 *
 * Embeds Google Maps Platform (Places, Routes, Geocoding, Live GPS, and Hazard Reports)
 * powered by your official Google Maps Web API key.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SmartMoveWebScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .statusBarsPadding(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setGeolocationEnabled(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            // Automatically grant GPS geolocation permissions to the embedded map
                            callback?.invoke(origin, true, false)
                        }
                    }

                    webViewClient = object : WebViewClient() {}

                    // Load from assets folder directly
                    loadUrl("file:///android_asset/smartmove_map.html")
                }
            }
        )
    }
}
