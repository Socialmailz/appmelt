package com.appmelt.builder.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightweightEditorScreen(
    file: File,
    initialContent: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var isDirty by remember { mutableStateOf(false) }

    // History for Undo / Redo
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    // Search & Replace states
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showReplace by remember { mutableStateOf(false) }

    fun updateContentWithHistory(newContent: String) {
        if (newContent != content) {
            undoStack.add(content)
            redoStack.clear()
            content = newContent
            isDirty = true
        }
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeLast()
            redoStack.add(content)
            content = previous
            isDirty = true
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.add(content)
            content = next
            isDirty = true
        }
    }

    fun handleReplaceAll() {
        if (searchQuery.isNotEmpty()) {
            val replaced = content.replace(searchQuery, replaceQuery)
            updateContentWithHistory(replaced)
        }
    }

    val lines = remember(content) { content.split("\n") }
    val lineCount = lines.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDirty) "Modified • ${file.parentFile?.name ?: ""}" else file.parentFile?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { handleUndo() },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { handleRedo() },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_redo_button")
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("editor_search_toggle_button")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            onSave(content)
                            isDirty = false
                        },
                        enabled = isDirty,
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save Changes",
                            tint = if (isDirty) MaterialTheme.colorScheme.primary else Color.Gray
                        )
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
                .background(Color(0xFF12141A))
        ) {
            // Search and Replace Floating Bar
            AnimatedVisibility(visible = showSearchBar) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Find in file...", color = Color.Gray, fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("editor_search_input"),
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF323846)
                                )
                            )
                            IconButton(
                                onClick = { showReplace = !showReplace },
                                modifier = Modifier.testTag("editor_toggle_replace_button")
                            ) {
                                Icon(
                                    Icons.Default.FindReplace,
                                    contentDescription = "Toggle Replace",
                                    tint = if (showReplace) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            }
                            IconButton(
                                onClick = {
                                    showSearchBar = false
                                    searchQuery = ""
                                },
                                modifier = Modifier.testTag("editor_close_search_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.LightGray)
                            }
                        }

                        if (showReplace) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = replaceQuery,
                                    onValueChange = { replaceQuery = it },
                                    placeholder = { Text("Replace with...", color = Color.Gray, fontSize = 13.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("editor_replace_input"),
                                    textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                        unfocusedBorderColor = Color(0xFF323846)
                                    )
                                )
                                Button(
                                    onClick = { handleReplaceAll() },
                                    enabled = searchQuery.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("editor_replace_all_button")
                                ) {
                                    Text("Replace", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Editor Area: Gutter with Line Numbers + Monospaced Code Text
            val vScrollState = rememberScrollState()
            val hScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(vScrollState)
                    .horizontalScroll(hScrollState)
                    .padding(vertical = 8.dp)
            ) {
                // Line numbers gutter
                Column(
                    modifier = Modifier
                        .background(Color(0xFF181B24))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = "$i",
                            color = Color(0xFF5A6275),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Editable Code Content
                BasicTextField(
                    value = content,
                    onValueChange = { updateContentWithHistory(it) },
                    textStyle = TextStyle(
                        color = Color(0xFFE6EDF3),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp)
                        .testTag("editor_text_field")
                )
            }
        }
    }
}
