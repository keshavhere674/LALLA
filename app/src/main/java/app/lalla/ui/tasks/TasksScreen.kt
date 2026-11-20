package app.lalla.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.lalla.domain.model.LallaTask
import app.lalla.domain.model.TaskType
import app.lalla.ui.theme.CyanAccent
import app.lalla.ui.theme.DarkBackground
import app.lalla.ui.theme.DarkSurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavController,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    var showAll by remember { mutableStateOf(false) }
    val displayTasks = if (showAll) allTasks else pendingTasks

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LALLA Tasks & Logs",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    FilterChip(
                        selected = showAll,
                        onClick = { showAll = !showAll },
                        label = { Text(if (showAll) "All Logs" else "Pending", fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (displayTasks.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CyanAccent.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (showAll) "No task logs yet" else "No pending tasks",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Say \"LALLA, alarm laga do 6:03 AM\"\nor \"LALLA, Papa ko WhatsApp bhejo\"",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${displayTasks.size} ${if (showAll) "total execution log" else "pending task"}${if (displayTasks.size != 1) "s" else ""}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(displayTasks, key = { it.id }) { task ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            TaskCard(
                                task = task,
                                onComplete = { viewModel.markComplete(task.id) },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: LallaTask,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, accentColor) = taskIconAndColor(task.type)
    val isCompleted = task.isCompleted

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                DarkSurfaceVariant.copy(alpha = 0.5f)
            else
                DarkSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isCompleted) accentColor.copy(alpha = 0.4f) else accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.description,
                    color = if (isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskChip(task.type.name, accentColor)

                    task.triggerTimeMs?.let { ms ->
                        TaskChip(
                            SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(ms)),
                            CyanAccent
                        )
                    }

                    task.targetContact?.let { contact ->
                        TaskChip("-> $contact", Color(0xFF25D366))
                    }

                    task.executionMethod?.let { method ->
                        TaskChip(method, Color(0xFF81D4FA))
                    }
                }

                if (task.resultStatus != null || isCompleted) {
                    Spacer(Modifier.height(3.dp))
                    val statusText = when (task.resultStatus) {
                        "SUCCESS" -> "✓ Executed successfully"
                        "FALLBACK_SUCCESS" -> "✓ Executed via fallback"
                        "FAILED" -> "✗ Failed: ${task.failureReason ?: "Error"}"
                        else -> "✓ Completed"
                    }
                    val statusColor = if (task.resultStatus == "FAILED") Color(0xFFFF5252) else Color(0xFF00E676)
                    Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (!isCompleted) {
                IconButton(onClick = onComplete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Check, "Complete", tint = Color(0xFF00E676))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun TaskChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private fun taskIconAndColor(type: TaskType): Pair<ImageVector, Color> = when (type) {
    TaskType.ALARM        -> Icons.Default.Alarm to Color(0xFFFFB300)
    TaskType.REMINDER     -> Icons.Default.NotificationsActive to Color(0xFFCE93D8)
    TaskType.WHATSAPP     -> Icons.AutoMirrored.Filled.Message to Color(0xFF25D366)
    TaskType.GMAIL        -> Icons.Default.Email to Color(0xFFEA4335)
    TaskType.FILE_SEARCH  -> Icons.Default.Folder to Color(0xFFFF9800)
    TaskType.CALL_HISTORY -> Icons.Default.Phone to CyanAccent
    TaskType.GENERIC      -> Icons.Default.Task to CyanAccent
}
