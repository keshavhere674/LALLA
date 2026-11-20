package app.lalla.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.lalla.data.ai.RoutingAiEngine
import app.lalla.data.ai.local.ModelInfo
import app.lalla.data.ai.local.ModelStatus
import app.lalla.ui.theme.CyanAccent
import app.lalla.ui.theme.DarkBackground
import app.lalla.ui.theme.DarkSurfaceVariant
import app.lalla.ui.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = VividViolet)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold, color = Color.White
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Background Wake-Word Listening (Master Control)
            SettingsCard(title = "Background Wake-Word Listening") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isBackgroundListeningEnabled) "Active: Listening for \"LALLA\"" else "Disabled: Microphone OFF",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.isBackgroundListeningEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "Say \"LALLA\" even when the app is closed to set alarms, send WhatsApp, or check call logs.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = uiState.isBackgroundListeningEnabled,
                        onCheckedChange = { viewModel.toggleBackgroundListening() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E676),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                if (uiState.isBackgroundListeningEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Sensitivity: ${uiState.wakeWordSensitivity.toInt()}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Slider(
                        value = uiState.wakeWordSensitivity,
                        onValueChange = { viewModel.updateWakeWordSensitivity(it) },
                        valueRange = 500f..2500f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = DarkBackground
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quiet Room (Low)", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text("Default", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text("Noisy (High)", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    }
                }
            }
            // ── AI Engine Selector ──────────────────────────────────────────
            SettingsCard(title = "Active AI Engine") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EngineChip(
                        label = "On-Device",
                        emoji = "📱",
                        selected = uiState.provider == RoutingAiEngine.PROVIDER_LOCAL,
                        onClick = { viewModel.selectProvider(RoutingAiEngine.PROVIDER_LOCAL) }
                    )
                    EngineChip(
                        label = "Ollama",
                        emoji = "🏠",
                        selected = uiState.provider == RoutingAiEngine.PROVIDER_OLLAMA,
                        onClick = { viewModel.selectProvider(RoutingAiEngine.PROVIDER_OLLAMA) }
                    )
                    EngineChip(
                        label = "Cloud",
                        emoji = "☁️",
                        selected = uiState.provider == RoutingAiEngine.PROVIDER_FREELLM,
                        onClick = { viewModel.selectProvider(RoutingAiEngine.PROVIDER_FREELLM) }
                    )
                    EngineChip(
                        label = "Demo",
                        emoji = "🤖",
                        selected = uiState.provider == RoutingAiEngine.PROVIDER_MOCK,
                        onClick = { viewModel.selectProvider(RoutingAiEngine.PROVIDER_MOCK) }
                    )
                }
            }

            // ── Engine Config ───────────────────────────────────────────────
            SettingsCard(title = when (uiState.provider) {
                RoutingAiEngine.PROVIDER_LOCAL -> "On-Device Standalone AI"
                RoutingAiEngine.PROVIDER_OLLAMA -> "Ollama Local Engine"
                RoutingAiEngine.PROVIDER_FREELLM -> "FreeLLMAPI / OpenAI Endpoint"
                else -> "Demo Mock Mode"
            }) {
                when (uiState.provider) {
                    RoutingAiEngine.PROVIDER_LOCAL -> LocalModelConfig(uiState.modelInfo) { viewModel.refreshModelInfo() }
                    RoutingAiEngine.PROVIDER_OLLAMA -> OllamaConfig(uiState, viewModel)
                    RoutingAiEngine.PROVIDER_FREELLM -> FreeLlmConfig(uiState, viewModel)
                    else -> {
                        Text(
                            text = "Generates simulated multilingual responses directly on-device — no server required.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Temperature slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Creativity (Temperature)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                    Text(
                        text = String.format("%.1f", uiState.temperature),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold, color = VividViolet
                        )
                    )
                }
                Slider(
                    value = uiState.temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    valueRange = 0.0f..1.0f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = VividViolet,
                        activeTrackColor = VividViolet,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VividViolet)
                ) {
                    if (uiState.isSaved) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Saved!", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Save Changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── About ───────────────────────────────────────────────────────
            SettingsCard(title = "About LALLA") {
                Text(
                    text = "LALLA – Your Personal AI Companion\nVersion 0.3.0 (Standalone On-Device AI)\n100% Offline · Private · Works without PC or USB",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun LocalModelConfig(modelInfo: ModelInfo, onRefresh: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (modelInfo.status) {
                ModelStatus.READY -> CyanAccent
                ModelStatus.NOT_FOUND -> MaterialTheme.colorScheme.error
                ModelStatus.ERROR -> MaterialTheme.colorScheme.error
                ModelStatus.VALIDATING -> VividViolet
            }
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (modelInfo.status) {
                    ModelStatus.READY -> "Model Ready (${modelInfo.fileSizeMb} MB)"
                    ModelStatus.NOT_FOUND -> "Model Not Installed"
                    ModelStatus.ERROR -> "Model Invalid"
                    ModelStatus.VALIDATING -> "Validating..."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Refresh, "Refresh", tint = VividViolet, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Model: ${modelInfo.fileName}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Path: ${modelInfo.filePath}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Runs 100% on-device inside your phone using native llama.cpp. No internet, PC, or server needed.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = VividViolet
                )
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun EngineChip(label: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text("$emoji $label", fontSize = 12.sp)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VividViolet.copy(alpha = 0.25f),
            selectedLabelColor = VividViolet,
            containerColor = DarkBackground,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = VividViolet,
            borderColor = DarkSurfaceVariant
        )
    )
}

@Composable
fun OllamaConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    // Connection status indicator
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        val dotColor = when (uiState.connectionStatus) {
            SettingsConnectionStatus.CONNECTED    -> CyanAccent
            SettingsConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
            SettingsConnectionStatus.CHECKING     -> VividViolet
            SettingsConnectionStatus.UNKNOWN      -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        if (uiState.connectionStatus == SettingsConnectionStatus.CHECKING) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = VividViolet
            )
        } else {
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = when (uiState.connectionStatus) {
                SettingsConnectionStatus.CONNECTED    -> "Ollama Connected ✓"
                SettingsConnectionStatus.DISCONNECTED -> "Ollama Unreachable"
                SettingsConnectionStatus.CHECKING     -> "Checking..."
                SettingsConnectionStatus.UNKNOWN      -> "Not tested"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = dotColor,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { viewModel.testConnection() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Refresh, "Test", tint = VividViolet, modifier = Modifier.size(18.dp))
        }
    }

    SettingsTextField(
        value = uiState.ollamaUrl,
        label = "Ollama URL",
        onChange = { viewModel.updateOllamaUrl(it) }
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallPresetChip("ADB Reverse", uiState.ollamaUrl == "http://localhost:11434") {
            viewModel.updateOllamaUrl("http://localhost:11434")
        }
        SmallPresetChip("Emulator", uiState.ollamaUrl == "http://10.0.2.2:11434") {
            viewModel.updateOllamaUrl("http://10.0.2.2:11434")
        }
    }
    Spacer(Modifier.height(10.dp))
    SettingsTextField(
        value = uiState.ollamaModel,
        label = "Model Identifier",
        onChange = { viewModel.updateOllamaModel(it) }
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallPresetChip("Qwen 2.5 3B", uiState.ollamaModel == "qwen2.5:3b") {
            viewModel.updateOllamaModel("qwen2.5:3b")
        }
        SmallPresetChip("Qwen 2.5 7B", uiState.ollamaModel == "qwen2.5:7b") {
            viewModel.updateOllamaModel("qwen2.5:7b")
        }
    }
}

@Composable
fun FreeLlmConfig(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsTextField(
        value = uiState.freeLlmUrl,
        label = "Base URL",
        onChange = { viewModel.updateFreeLlmUrl(it) }
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallPresetChip("Local :3000", uiState.freeLlmUrl == "http://localhost:3000/v1") {
            viewModel.updateFreeLlmUrl("http://localhost:3000/v1")
        }
        SmallPresetChip("Groq Cloud", uiState.freeLlmUrl == "https://api.groq.com/openai/v1") {
            viewModel.updateFreeLlmUrl("https://api.groq.com/openai/v1")
        }
    }
    Spacer(Modifier.height(10.dp))
    SettingsTextField(
        value = uiState.freeLlmModel,
        label = "Model Name",
        onChange = { viewModel.updateFreeLlmModel(it) }
    )
    Spacer(Modifier.height(10.dp))
    SettingsTextField(
        value = uiState.freeLlmApiKey,
        label = "API Key (optional for local)",
        onChange = { viewModel.updateFreeLlmApiKey(it) }
    )
}

@Composable
fun SettingsTextField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VividViolet,
            unfocusedBorderColor = DarkBackground,
            focusedContainerColor = DarkBackground,
            unfocusedContainerColor = DarkBackground,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = VividViolet
        )
    )
}

@Composable
fun SmallPresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VividViolet.copy(alpha = 0.2f),
            selectedLabelColor = VividViolet,
            containerColor = DarkBackground,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = VividViolet,
            borderColor = DarkSurfaceVariant.copy(alpha = 0.5f)
        )
    )
}
