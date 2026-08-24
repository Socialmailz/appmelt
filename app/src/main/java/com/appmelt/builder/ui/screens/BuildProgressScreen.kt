package com.appmelt.builder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appmelt.builder.model.BuildLogItem
import com.appmelt.builder.model.BuildResult
import com.appmelt.builder.model.BuildStep
import com.appmelt.builder.model.ErrorActionType
import com.appmelt.builder.model.FriendlyError
import com.appmelt.builder.model.LogLevel
import com.appmelt.builder.model.Project
import com.appmelt.builder.ui.theme.DarkBorder
import com.appmelt.builder.ui.theme.DarkSurface
import com.appmelt.builder.ui.theme.DarkSurfaceElevated
import com.appmelt.builder.ui.theme.ErrorRed
import com.appmelt.builder.ui.theme.SuccessGreen
import com.appmelt.builder.ui.theme.TextPrimary
import com.appmelt.builder.ui.theme.TextSecondary
import com.appmelt.builder.ui.theme.WarningAmber
import com.appmelt.builder.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildProgressScreen(
    viewModel: MainViewModel,
    project: Project?,
    isBuilding: Boolean,
    progressPercent: Int,
    currentStepTitle: String,
    steps: List<BuildStep>,
    logs: List<BuildLogItem>,
    lastResult: BuildResult?,
    onViewLogs: () -> Unit,
    onBack: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        label = "build_progress"
    )

    val logListState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBuilding) "Building ${project?.name ?: "App"}..." else if (lastResult?.isSuccess == false) "Build Failed" else "Build Complete",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBuilding) currentStepTitle else if (lastResult?.isSuccess == false) "Tap diagnosis to fix" else "Generated APK ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lastResult?.isSuccess == false) ErrorRed else TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("progress_back_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onViewLogs, modifier = Modifier.testTag("progress_view_logs_button")) {
                        Icon(Icons.Default.Description, contentDescription = "Logs", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBuilding) "Local Build Pipeline Active" else if (lastResult?.isSuccess == false) "Pipeline Stopped" else "Pipeline Complete",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "$progressPercent%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lastResult?.isSuccess == false) ErrorRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("progress_percentage_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("build_linear_progress"),
                            color = if (lastResult?.isSuccess == false) ErrorRed else MaterialTheme.colorScheme.primary,
                            trackColor = DarkSurfaceElevated
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentStepTitle,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.testTag("current_step_text")
                        )
                    }
                }
            }

            // Error Translation Card (If Build Failed)
            if (!isBuilding && lastResult?.isSuccess == false && lastResult.friendlyError != null) {
                item {
                    val error = lastResult.friendlyError
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1518)),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = error.summary,
                                fontSize = 13.sp,
                                color = Color(0xFFFFD0D5),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (error.actionText != null) {
                                    Button(
                                        onClick = {
                                            when (error.actionType) {
                                                ErrorActionType.INSTALL_SDK -> {
                                                    viewModel.installRuntime(error.actionPayload)
                                                    viewModel.startBuild()
                                                }
                                                ErrorActionType.SWITCH_JDK -> {
                                                    viewModel.installRuntime(error.actionPayload)
                                                    viewModel.startBuild()
                                                }
                                                ErrorActionType.DOWNLOAD_DEPENDENCIES -> {
                                                    viewModel.cleanCache()
                                                    viewModel.startBuild()
                                                }
                                                ErrorActionType.ASK_AI -> {
                                                    viewModel.triggerAiDiagnosisForError(error)
                                                }
                                                else -> {
                                                    viewModel.startBuild()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ErrorRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("friendly_error_action_button")
                                    ) {
                                        Text(text = error.actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.triggerAiDiagnosisForError(error) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("friendly_error_ask_ai_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ask AI", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Step Pipeline Checklist
            item {
                Text(
                    text = "Build Pipeline Steps",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            items(steps) { step ->
                StepChecklistItem(step = step)
            }

            // Live Log Console Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10131A)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Build Logs",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "View All (${logs.size})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onViewLogs)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(Color(0xFF090B0E), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(state = logListState) {
                                items(logs.takeLast(30)) { log ->
                                    val color = when (log.level) {
                                        LogLevel.SUCCESS -> SuccessGreen
                                        LogLevel.WARN -> WarningAmber
                                        LogLevel.ERROR -> ErrorRed
                                        else -> Color(0xFFC0CAD5)
                                    }
                                    Text(
                                        text = "${log.timestamp} ${log.message}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = color,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun StepChecklistItem(step: BuildStep) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (step.isRunning) MaterialTheme.colorScheme.primary else if (step.isFailed) ErrorRed else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                step.isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                step.isRunning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                step.isFailed -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(DarkBorder)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = step.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (step.isRunning) MaterialTheme.colorScheme.primary else TextPrimary
                )
                Text(
                    text = step.description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
