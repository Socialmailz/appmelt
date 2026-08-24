package com.appmelt.builder.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appmelt.builder.model.BuildLogItem
import com.appmelt.builder.model.LogLevel
import com.appmelt.builder.ui.theme.DarkBorder
import com.appmelt.builder.ui.theme.ErrorRed
import com.appmelt.builder.ui.theme.SuccessGreen
import com.appmelt.builder.ui.theme.TextPrimary
import com.appmelt.builder.ui.theme.TextSecondary
import com.appmelt.builder.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildLogsScreen(
    logs: List<BuildLogItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }

    val filteredLogs = remember(logs, searchQuery, selectedLevel) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isEmpty() || log.message.contains(searchQuery, ignoreCase = true)
            val matchesLevel = selectedLevel == null || log.level == selectedLevel
            matchesSearch && matchesLevel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Build Logs (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("logs_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = logs.joinToString("\n") { "${it.timestamp} [${it.level}] ${it.message}" }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Build Logs", text))
                            Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_logs_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0C0E14))
                .padding(12.dp)
        ) {
            // Search and filter row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter logs by keyword...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logs_search_input"),
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = Color(0xFF141720),
                    unfocusedContainerColor = Color(0xFF141720)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Level Filter Chips
            val hScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedLevel == null,
                    onClick = { selectedLevel = null },
                    label = { Text("ALL", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.INFO,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.INFO) null else LogLevel.INFO },
                    label = { Text("INFO", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.SUCCESS,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.SUCCESS) null else LogLevel.SUCCESS },
                    label = { Text("SUCCESS", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.WARN,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.WARN) null else LogLevel.WARN },
                    label = { Text("WARN", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.ERROR,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.ERROR) null else LogLevel.ERROR },
                    label = { Text("ERROR", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Log Console
            val logHScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07090D), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .horizontalScroll(logHScroll)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredLogs) { log ->
                        val color = when (log.level) {
                            LogLevel.SUCCESS -> SuccessGreen
                            LogLevel.WARN -> WarningAmber
                            LogLevel.ERROR -> ErrorRed
                            else -> Color(0xFFCBD5E1)
                        }
                        Text(
                            text = "${log.timestamp}  ${log.message}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
