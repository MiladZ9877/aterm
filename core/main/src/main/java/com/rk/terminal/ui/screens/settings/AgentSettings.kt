package com.rk.terminal.ui.screens.settings

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.settings.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettings() {
    var enableStreaming by remember { mutableStateOf(Settings.enable_streaming) }
    
    PreferenceGroup(heading = "Agent Settings") {
        SettingsCard(
            title = { Text("Enable Streaming Mode") },
            description = { 
                Text(
                    if (enableStreaming) {
                        "Agent responses stream in real-time. Disable for metadata-first approach."
                    } else {
                        "Metadata-first mode: generates file structure first, then code. Better for complex projects."
                    }
                )
            },
            startWidget = {
                Switch(
                    checked = enableStreaming,
                    onCheckedChange = {
                        enableStreaming = it
                        Settings.enable_streaming = it
                    }
                )
            },
            onClick = {
                enableStreaming = !enableStreaming
                Settings.enable_streaming = enableStreaming
            }
        )
    }
}
