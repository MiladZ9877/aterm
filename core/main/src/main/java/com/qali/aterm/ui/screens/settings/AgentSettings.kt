package com.qali.aterm.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.settings.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettings() {
    var enableStreaming by remember { mutableStateOf(Settings.enable_streaming) }
    var useApiSearch by remember { mutableStateOf(Settings.use_api_search) }
    var recursiveCurls by remember { mutableStateOf(Settings.custom_search_recursive_curls) }
    
    PreferenceGroup(heading = "Agent Settings") {
        SettingsCard(
            title = { Text("Enable Streaming Mode") },
            description = { 
                Column {
                    Text(
                        if (enableStreaming) {
                            "Streaming Mode: Responses appear in real-time as they're generated. Faster feedback, better for interactive tasks."
                        } else {
                            "Non-Streaming Mode: Complete responses after full generation. Better for complex projects requiring full context. May show duplicate info messages."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!enableStreaming) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Note: In non-streaming mode, you may see duplicate information messages. This is expected behavior.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
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
        
        SettingsCard(
            title = { Text("Use API Search") },
            description = { 
                Text(
                    if (useApiSearch) {
                        "Uses Gemini API's built-in Google Search. Faster but requires API support."
                    } else {
                        "Uses custom search engine with direct API calls and AI analysis. More control and recursive searches."
                    }
                )
            },
            startWidget = {
                Switch(
                    checked = useApiSearch,
                    onCheckedChange = {
                        useApiSearch = it
                        Settings.use_api_search = it
                    }
                )
            },
            onClick = {
                useApiSearch = !useApiSearch
                Settings.use_api_search = useApiSearch
            }
        )
        
        if (!useApiSearch) {
            SettingsCard(
                title = { Text("Custom Search Recursive Levels") },
                description = { 
                    Column {
                        Text(
                            "Number of recursive search levels (1-8). Higher values gather more comprehensive information but take longer. Current: $recursiveCurls"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = recursiveCurls.toFloat(),
                            onValueChange = { newValue ->
                                recursiveCurls = newValue.toInt().coerceIn(1, 8)
                                Settings.custom_search_recursive_curls = recursiveCurls
                            },
                            valueRange = 1f..8f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Levels: $recursiveCurls",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                onClick = { /* No action needed, slider handles interaction */ }
            )
        }
    }
}
