package com.example.urlwrap

import android.os.Bundle
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.urlwrap.UrlDataStore
import com.example.urlwrap.ui.theme.URLWrapTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null  // Store WebView reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            URLWrapTheme {
                val context = LocalContext.current
                val dataStore = UrlDataStore(context)
                val scope = rememberCoroutineScope()
                var url by remember { mutableStateOf("https://calendar.google.com") }
                var showDialog by remember { mutableStateOf(false) }

                // Load saved URL from DataStore when app starts
                LaunchedEffect(Unit) {
                    dataStore.getUrl.collectLatest { savedUrl ->
                        if (savedUrl.isNotEmpty()) {
                            url = savedUrl
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WebViewScreen(
                        url = url,
                        modifier = Modifier.padding(innerPadding),
                        onFourFingerTap = { showDialog = true }, // Trigger dialog on 4-finger tap
                        webViewReference = { webView = it }
                    )
                }

                if (showDialog) {
                    UrlInputDialog(
                        currentUrl = url,
                        onUrlChange = { newUrl: String ->
                            scope.launch {
                                dataStore.saveUrl(newUrl)
                            }
                            url = newUrl  // Immediately update WebView
                        },
                        onDismiss = { showDialog = false }
                    )
                }

                // Handle system back button properly
                HandleSystemBackButton()
            }
        }
    }

    @Composable
    fun HandleSystemBackButton() {
        BackHandler(enabled = true) {
            webView?.let {
                if (it.canGoBack()) {
                    it.goBack() // Navigate back inside WebView
                } else {
                    finish() // Exit app if no history available
                }
            } ?: finish() // If WebView is null, exit app
        }
    }
}

@Composable
fun WebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    onFourFingerTap: () -> Unit,
    webViewReference: (WebView) -> Unit
) {
    val context = LocalContext.current
    val updatedUrl by rememberUpdatedState(url)

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(updatedUrl)

                webViewReference(this)  // Store WebView instance for back navigation

                // Detect 4-finger tap
                setOnTouchListener { _, event ->
                    if (event.pointerCount == 4 &&
                        (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
                        onFourFingerTap()
                        performClick() // Fix accessibility warning
                        return@setOnTouchListener true
                    }
                    false
                }
            }
        },
        update = { webViewInstance ->
            webViewInstance.loadUrl(updatedUrl)
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun UrlInputDialog(
    currentUrl: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Enter New URL") },
        text = {
            Column {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onUrlChange(text)  // Pass new URL back to MainActivity
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}
