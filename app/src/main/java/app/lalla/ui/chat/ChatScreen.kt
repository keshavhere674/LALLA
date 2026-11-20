package app.lalla.ui.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.lalla.domain.model.Conversation
import app.lalla.domain.model.Message
import app.lalla.domain.model.MessageRole
import app.lalla.ui.navigation.LallaDestinations
import app.lalla.ui.theme.CyanAccent
import app.lalla.ui.theme.DarkBackground
import app.lalla.ui.theme.DarkSurfaceVariant
import app.lalla.ui.theme.UserBubble
import app.lalla.ui.theme.VividViolet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
    pendingVoiceCommand: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val allConversations by viewModel.allConversations.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle voice command delivered from background service
    LaunchedEffect(pendingVoiceCommand) {
        if (!pendingVoiceCommand.isNullOrBlank()) {
            inputText = pendingVoiceCommand
            viewModel.sendMessage(pendingVoiceCommand)
        }
    }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceInput()
    }

    // Auto-scroll on new messages / streaming
    LaunchedEffect(uiState.messages.size, uiState.streamingResponse) {
        val totalCount = uiState.messages.size + if (uiState.isLoading) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    // Fill input from voice recognition partial text
    LaunchedEffect(uiState.voiceInputText) {
        if (uiState.voiceInputText.isNotBlank()) {
            inputText = uiState.voiceInputText
        }
    }

    // ── Navigation Drawer (Chat History) ─────────────────────────────────────
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationHistoryDrawer(
                conversations = allConversations,
                currentConversationId = uiState.currentConversationId,
                onNewChat = {
                    viewModel.startNewConversation()
                    scope.launch { drawerState.close() }
                },
                onSelectConversation = { conv ->
                    viewModel.switchConversation(conv.id)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "LALLA",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                "Your Personal AI Companion",
                                fontSize = 11.sp,
                                color = CyanAccent.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        // Hamburger menu → chat history drawer
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Chat History", tint = Color.White)
                        }
                    },
                    actions = {
                        // TTS toggle
                        IconButton(onClick = { viewModel.toggleTts() }) {
                            Icon(
                                imageVector = if (uiState.isTtsEnabled)
                                    Icons.AutoMirrored.Filled.VolumeUp
                                else
                                    Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Toggle TTS",
                                tint = if (uiState.isTtsEnabled) CyanAccent else Color.Gray
                            )
                        }
                        // Tasks button
                        IconButton(onClick = { navController.navigate(LallaDestinations.TASKS) }) {
                            Icon(Icons.Default.Task, "Tasks", tint = Color.White)
                        }
                        // Memory button
                        IconButton(onClick = { navController.navigate(LallaDestinations.MEMORY) }) {
                            Icon(Icons.Default.Psychology, "Memory", tint = Color.White)
                        }
                        // Settings
                        IconButton(onClick = { navController.navigate(LallaDestinations.SETTINGS) }) {
                            Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Error banner
                AnimatedVisibility(visible = uiState.error != null) {
                    uiState.error?.let { errorMsg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { viewModel.clearError() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    errorMsg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Voice state banner
                AnimatedVisibility(
                    visible = uiState.voiceState != VoiceState.IDLE,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    VoiceStateBanner(uiState.voiceState, uiState.voiceInputText)
                }

                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.messages.isEmpty() && !uiState.isLoading) {
                        item {
                            WelcomeMessage()
                        }
                    }

                    items(uiState.messages) { message ->
                        MessageBubble(message = message)
                    }

                    // Streaming bubble
                    if (uiState.isLoading && uiState.streamingResponse.isNotBlank()) {
                        item {
                            StreamingBubble(text = uiState.streamingResponse)
                        }
                    } else if (uiState.isLoading) {
                        item {
                            ThinkingBubble()
                        }
                    }
                }

                // Input bar
                InputBar(
                    inputText = inputText,
                    isLoading = uiState.isLoading,
                    voiceState = uiState.voiceState,
                    isTtsEnabled = uiState.isTtsEnabled,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onMicClick = {
                        when (uiState.voiceState) {
                            VoiceState.LISTENING -> viewModel.stopVoiceInput()
                            VoiceState.SPEAKING -> viewModel.stopSpeaking()
                            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }
    }
}

// ── Conversation History Drawer ───────────────────────────────────────────────

@Composable
private fun ConversationHistoryDrawer(
    conversations: List<Conversation>,
    currentConversationId: Long,
    onNewChat: () -> Unit,
    onSelectConversation: (Conversation) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0D1117),
        modifier = Modifier.fillMaxHeight()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(VividViolet.copy(alpha = 0.6f), DarkBackground),
                        start = Offset(0f, 0f),
                        end = Offset(300f, 200f)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "LALLA",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Text(
                    "Chat History",
                    color = CyanAccent.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        // New Chat button
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Add, null, tint = CyanAccent) },
            label = {
                Text(
                    "New Chat",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            selected = false,
            onClick = onNewChat,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(4.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No conversations yet",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(conversations, key = { it.id }) { conv ->
                    val isSelected = conv.id == currentConversationId
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Default.ChatBubble,
                                null,
                                tint = if (isSelected) VividViolet else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Column {
                                Text(
                                    conv.title,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                        .format(Date(conv.createdAt)),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = { onSelectConversation(conv) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = VividViolet.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Voice State Banner ────────────────────────────────────────────────────────

@Composable
private fun VoiceStateBanner(voiceState: VoiceState, voiceInputText: String) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseVal"
    )

    val (bg, label) = when (voiceState) {
        VoiceState.LISTENING -> CyanAccent.copy(alpha = 0.15f) to "🎤 Listening..."
        VoiceState.THINKING  -> VividViolet.copy(alpha = 0.15f) to "🤔 Thinking..."
        VoiceState.SPEAKING  -> Color(0xFF25D366).copy(alpha = 0.12f) to "🔊 Speaking..."
        VoiceState.IDLE      -> Color.Transparent to ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(10.dp)
    ) {
        Column {
            Text(
                label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.scale(pulse)
            )
            if (voiceInputText.isNotBlank()) {
                Text(
                    voiceInputText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Welcome Message ───────────────────────────────────────────────────────────

@Composable
private fun WelcomeMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(VividViolet.copy(alpha = 0.4f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = VividViolet,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Namaste! Main LALLA hoon 👋",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Kuch bhi poochein ya bolein.\nBackground mein bhi sun raha hoon — bas bolein \"LALLA\" aur apna kaam batayein!",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            // Quick suggestions
            listOf(
                "🕐 Aaj kya date hai?",
                "⏰ Alarm laga do 7 baje",
                "💬 Maa ko WhatsApp bhejo",
                "📋 Meri tasks dikhao"
            ).forEach { suggestion ->
                Text(
                    suggestion,
                    color = CyanAccent.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VividViolet, CyanAccent),
                            start = Offset(0f, 0f),
                            end = Offset(32f, 32f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("L", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (isUser) UserBubble else DarkSurfaceVariant
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Text(
                formatTime(message.timestamp),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Streaming / Thinking bubbles ──────────────────────────────────────────────

@Composable
private fun StreamingBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(VividViolet, CyanAccent))),
            contentAlignment = Alignment.Center
        ) {
            Text("L", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ThinkingBubble() {
    val inf = rememberInfiniteTransition(label = "dots")
    val dot by inf.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "dotVal"
    )
    val dotsStr = ".".repeat((dot.toInt() % 3) + 1) + "  ".substring(0, 2 - (dot.toInt() % 3))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(VividViolet, CyanAccent))),
            contentAlignment = Alignment.Center
        ) {
            Text("L", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = DarkSurfaceVariant
        ) {
            Text(
                "Soch raha hoon$dotsStr",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = CyanAccent.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

// ── Input Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    inputText: String,
    isLoading: Boolean,
    voiceState: VoiceState,
    isTtsEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Surface(color = DarkBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Mic button
            val micIcon = when (voiceState) {
                VoiceState.LISTENING -> Icons.Default.MicOff
                VoiceState.SPEAKING  -> Icons.Default.StopCircle
                else                 -> Icons.Default.Mic
            }
            val micTint = when (voiceState) {
                VoiceState.LISTENING, VoiceState.SPEAKING -> CyanAccent
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
