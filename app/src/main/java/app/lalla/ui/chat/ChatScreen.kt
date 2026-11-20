package app.lalla.ui.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.lalla.domain.model.Message
import app.lalla.domain.model.MessageRole
import app.lalla.ui.navigation.LallaDestinations
import app.lalla.ui.theme.CyanAccent
import app.lalla.ui.theme.DarkBackground
import app.lalla.ui.theme.DarkSurfaceVariant
import app.lalla.ui.theme.UserBubble
import app.lalla.ui.theme.VividViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceInput()
    }

    // Auto-scroll on new message or streaming update
    LaunchedEffect(uiState.messages.size, uiState.streamingResponse) {
        val totalCount = uiState.messages.size + if (uiState.isLoading) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animated LALLA avatar orb
                        LallaAvatarOrb(voiceState = uiState.voiceState, size = 36)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LALLA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = VividViolet
                                )
                            )
                            Text(
                                text = when (uiState.voiceState) {
                                    VoiceState.LISTENING -> "🎙️ Listening..."
                                    VoiceState.THINKING  -> "💭 Thinking..."
                                    VoiceState.SPEAKING  -> "🔊 Speaking..."
                                    VoiceState.IDLE      -> "Your Personal AI Companion"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (uiState.voiceState) {
                                        VoiceState.LISTENING -> CyanAccent
                                        VoiceState.THINKING  -> VividViolet
                                        VoiceState.SPEAKING  -> CyanAccent
                                        VoiceState.IDLE      -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            )
                        }
                    }
                },
                actions = {
                    // TTS toggle
                    IconButton(onClick = { viewModel.toggleTts() }) {
                        Icon(
                            imageVector = if (uiState.isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                                          else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle TTS",
                            tint = if (uiState.isTtsEnabled) CyanAccent
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { navController.navigate(LallaDestinations.MEMORY) }) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Memories",
                            tint = VividViolet
                        )
                    }
                    IconButton(onClick = { navController.navigate(LallaDestinations.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            BottomInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                voiceState = uiState.voiceState,
                isLoading = uiState.isLoading,
                voiceInputText = uiState.voiceInputText,
                error = uiState.error,
                onSend = {
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        val toSend = inputText
                        inputText = ""
                        viewModel.sendMessage(toSend)
                    }
                },
                onMicClick = {
                    when (uiState.voiceState) {
                        VoiceState.LISTENING -> viewModel.stopVoiceInput()
                        VoiceState.SPEAKING  -> viewModel.stopSpeaking()
                        else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onDismissError = { viewModel.clearError() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, Color(0xFF0F0F20))
                    )
                )
        ) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                EmptyStateGreeting(voiceState = uiState.voiceState)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            ChatBubble(message = message)
                        }
                    }

                    if (uiState.isLoading) {
                        item(key = "streaming_bubble") {
                            StreamingBubble(text = uiState.streamingResponse)
                        }
                    }
                }
            }
        }
    }
}

// ─── Animated LALLA Avatar Orb ───────────────────────────────────────────────
@Composable
fun LallaAvatarOrb(voiceState: VoiceState, size: Int = 48) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (voiceState) {
            VoiceState.LISTENING -> 1.25f
            VoiceState.SPEAKING  -> 1.15f
            VoiceState.THINKING  -> 1.08f
            VoiceState.IDLE      -> 1.0f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.LISTENING -> 600
                    VoiceState.SPEAKING  -> 800
                    VoiceState.THINKING  -> 1200
                    VoiceState.IDLE      -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    val orbColor = when (voiceState) {
        VoiceState.LISTENING -> CyanAccent
        VoiceState.SPEAKING  -> CyanAccent.copy(alpha = 0.85f)
        VoiceState.THINKING  -> VividViolet
        VoiceState.IDLE      -> VividViolet
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(orbColor, orbColor.copy(alpha = 0.4f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "L",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = (size * 0.45f).sp
            )
        )
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────
@Composable
fun EmptyStateGreeting(voiceState: VoiceState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LallaAvatarOrb(voiceState = voiceState, size = 88)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Namaste! Main LALLA hoon.",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aap mujhse Hindi, English ya Hinglish mein baat kar sakte hain.\nKuch bhi poochhein — main sab yaad rakhti hoon! 🧠",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HintChip("🎙️ Voice baat karein")
            HintChip("🧠 Memories dekhein")
        }
    }
}

@Composable
fun HintChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceVariant,
        tonalElevation = 2.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ─── Chat Bubble ─────────────────────────────────────────────────────────────
@Composable
fun ChatBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) UserBubble else DarkSurfaceVariant
    val textColor = Color.White
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(VividViolet, VividViolet.copy(alpha = 0.5f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("L", style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp
                    ))
                }
                Spacer(Modifier.width(5.dp))
                Text("LALLA", style = MaterialTheme.typography.labelSmall.copy(
                    color = VividViolet.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold
                ))
            }
        }
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = textColor
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

// ─── Streaming Bubble ────────────────────────────────────────────────────────
@Composable
fun StreamingBubble(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotsAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp, color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = VividViolet.copy(alpha = alpha)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (text.isBlank()) "LALLA soch rahi hai..." else "typing...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = VividViolet.copy(alpha = alpha)
                        )
                    )
                }
            }
        }
    }
}

// ─── Bottom Input Bar ────────────────────────────────────────────────────────
@Composable
fun BottomInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    voiceState: VoiceState,
    isLoading: Boolean,
    voiceInputText: String,
    error: String?,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .background(DarkBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Voice input preview
        AnimatedVisibility(visible = voiceState == VoiceState.LISTENING && voiceInputText.isNotBlank()) {
            Surface(
                color = CyanAccent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = "🎙️ $voiceInputText",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Error banner
        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                        Text("✕", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Mic / Stop button
            val micIcon = when (voiceState) {
                VoiceState.LISTENING -> Icons.Default.MicOff
                VoiceState.SPEAKING  -> Icons.Default.StopCircle
                else                 -> Icons.Default.Mic
            }
            val micTint = when (voiceState) {
                VoiceState.LISTENING -> CyanAccent
                VoiceState.SPEAKING  -> CyanAccent
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val micScale by rememberInfiniteTransition(label = "mic").animateFloat(
                initialValue = 1f, targetValue = 1.2f,
                animationSpec = if (voiceState == VoiceState.LISTENING)
                    infiniteRepeatable(tween(500), RepeatMode.Reverse)
                else
                    infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                label = "micScale"
            )

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .scale(if (voiceState == VoiceState.LISTENING) micScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (voiceState == VoiceState.LISTENING) CyanAccent.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .size(44.dp)
            ) {
                Icon(imageVector = micIcon, contentDescription = "Voice", tint = micTint)
            }

            Spacer(Modifier.width(4.dp))

            // Text field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                placeholder = {
                    Text(
                        "Baat karein...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VividViolet,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = VividViolet
                ),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(Modifier.width(4.dp))

            // Send button
            val canSend = inputText.isNotBlank() && !isLoading
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(
                                colors = listOf(VividViolet, Color(0xFF5B00FF)),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            )
                        else Brush.linearGradient(
                            colors = listOf(DarkSurfaceVariant, DarkSurfaceVariant)
                        )
                    )
            ) {
                if (isLoading && voiceState == VoiceState.THINKING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
