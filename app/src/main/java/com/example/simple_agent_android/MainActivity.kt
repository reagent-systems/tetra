package com.example.simple_agent_android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.simple_agent_android.ui.theme.SimpleAgentAndroidTheme
import androidx.compose.ui.unit.dp
import android.os.Build
import android.content.Context
import android.net.Uri
import com.example.simple_agent_android.BoundingBoxAccessibilityService
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleAgentAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val (serviceEnabled, setServiceEnabled) = remember { mutableStateOf(false) }
                        val (overlayActive, setOverlayActive) = remember { mutableStateOf(BoundingBoxAccessibilityService.isOverlayActive()) }
                        var verticalOffset by remember { mutableStateOf(BoundingBoxAccessibilityService.getVerticalOffset(this@MainActivity)) }
                        Text(text = "Vertical Offset: $verticalOffset px")
                        Slider(
                            value = verticalOffset.toFloat(),
                            onValueChange = {
                                verticalOffset = it.toInt()
                                BoundingBoxAccessibilityService.setVerticalOffset(this@MainActivity, verticalOffset)
                            },
                            valueRange = -100f..100f,
                            steps = 200
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        }) {
                            Text(text = getString(R.string.enable_accessibility_service))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!hasOverlayPermission(this@MainActivity)) {
                            Button(onClick = {
                                requestOverlayPermission(this@MainActivity)
                            }) {
                                Text(text = "Grant Overlay Permission")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(onClick = {
                            if (!hasOverlayPermission(this@MainActivity)) {
                                requestOverlayPermission(this@MainActivity)
                            } else {
                                if (BoundingBoxAccessibilityService.isOverlayActive()) {
                                    BoundingBoxAccessibilityService.stopOverlay()
                                    setOverlayActive(false)
                                } else {
                                    BoundingBoxAccessibilityService.startOverlay()
                                    setOverlayActive(true)
                                }
                            }
                        }) {
                            Text(text = if (overlayActive) "Stop Overlay" else getString(R.string.start_overlay))
                        }
                        var showJsonDialog by remember { mutableStateOf(false) }
                        var jsonOutput by remember { mutableStateOf("") }
                        Button(onClick = {
                            jsonOutput = BoundingBoxAccessibilityService.getInteractiveElementsJson()
                            showJsonDialog = true
                        }) {
                            Text(text = "Export Interactive Elements as JSON")
                        }
                        if (showJsonDialog) {
                            val scrollState = rememberScrollState()
                            AlertDialog(
                                onDismissRequest = { showJsonDialog = false },
                                title = { Text("Interactive Elements JSON") },
                                text = {
                                    Text(
                                        jsonOutput,
                                        modifier = Modifier.verticalScroll(scrollState)
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showJsonDialog = false }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimpleAgentAndroidTheme {
        Greeting("Android")
    }
}

fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.packageName)
        )
        context.startActivity(intent)
    }
}