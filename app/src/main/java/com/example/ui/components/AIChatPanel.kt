package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.models.ChangePlan
import com.example.data.ai.models.PlannerResult
import com.example.data.db.ChatMessageEntity
import com.example.data.models.AIOperationState
import com.example.ui.theme.*

@Composable
fun AIChatPanel(
    messages: List<ChatMessageEntity>,
    onSendMessage: (String) -> Unit,
    activePlan: PlannerResult? = null,
    activeChangePlan: ChangePlan? = null,
    aiOperationState: AIOperationState = AIOperationState.IDLE,
    onApprovePlan: () -> Unit = {},
    onRegeneratePlan: () -> Unit = {},
    onApplyChangePlan: () -> Unit = {},
    onRejectChangePlan: () -> Unit = {},
    onCancelChangePlan: () -> Unit = {},
    onRollback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showPlanView by remember { mutableStateOf(activePlan != null) }
    var showChangeReview by remember { mutableStateOf(activeChangePlan != null) }
    val listState = rememberLazyListState()

    LaunchedEffect(activePlan) {
        if (activePlan != null) {
            showPlanView = true
        }
    }

    LaunchedEffect(activeChangePlan) {
        if (activeChangePlan != null) {
            showChangeReview = true
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Plan a rice trading web application",
        "Add a login page with auth",
        "Plan an inventory management system",
        "Create an order system plan",
        "Fix errors in current project",
        "Analyze architectural bottlenecks"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Agent Architecture Status & Plan Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "EFRAHOPE AI ENGINE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (activeChangePlan != null) {
                        Surface(
                            onClick = { showChangeReview = !showChangeReview },
                            shape = RoundedCornerShape(6.dp),
                            color = if (showChangeReview) Color(0xFF16A34A) else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = if (showChangeReview) Color.White else Color(0xFF16A34A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (showChangeReview) "Hide Changes" else "Review Changes",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showChangeReview) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    if (activePlan != null) {
                        Surface(
                            onClick = { showPlanView = !showPlanView },
                            shape = RoundedCornerShape(6.dp),
                            color = if (showPlanView) ElectricIndigo else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = if (showPlanView) Color.White else ElectricIndigo,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (showPlanView) "Hide Plan" else "View Plan",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showPlanView) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onRollback,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Rollback",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Undo",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // AI Status Bar if Operation Active
            if (aiOperationState != AIOperationState.IDLE) {
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = ElectricIndigo,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = when (aiOperationState) {
                                AIOperationState.ANALYZING -> "Analyzing project context..."
                                AIOperationState.PLANNING -> "Creating structured project plan..."
                                AIOperationState.GENERATING -> "Generating code & UI components..."
                                AIOperationState.VALIDATING -> "Validating code syntax..."
                                else -> "AI Agent Processing..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline)

            // Main View Area: ChangeReviewView or Plan View or Chat Messages
            if (showChangeReview && activeChangePlan != null) {
                Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                    ChangeReviewView(
                        plan = activeChangePlan,
                        onApply = {
                            onApplyChangePlan()
                            showChangeReview = false
                        },
                        onReject = {
                            onRejectChangePlan()
                            showChangeReview = false
                        },
                        onCancel = {
                            onCancelChangePlan()
                            showChangeReview = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (showPlanView && activePlan != null) {
                Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                    ProjectPlanView(
                        plan = activePlan,
                        onApprovePlan = onApprovePlan,
                        onRegeneratePlan = onRegeneratePlan,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Chat Message List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        ChatMessageItem(msg = msg)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline)

            // Quick Prompt Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Surface(
                        onClick = { onSendMessage(prompt) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("prompt_chip_${prompt.take(10).lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask AI to plan or build application...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    singleLine = false
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo)
                        .testTag("send_ai_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(msg: ChatMessageEntity) {
    val isUser = msg.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 9.sp)
                }
            }
            Text(
                text = msg.agentName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isUser) ElectricIndigo else CyberCyan
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            color = if (isUser) ElectricIndigo else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
