package com.appmelt.builder.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appmelt.builder.model.BuildVariant
import com.appmelt.builder.model.Project
import com.appmelt.builder.model.ProjectAnalysis
import com.appmelt.builder.ui.components.StatItem
import com.appmelt.builder.ui.components.StatusBadge
import com.appmelt.builder.ui.theme.DarkBorder
import com.appmelt.builder.ui.theme.DarkSurface
import com.appmelt.builder.ui.theme.DarkSurfaceElevated
import com.appmelt.builder.ui.theme.SuccessGreen
import com.appmelt.builder.ui.theme.TextPrimary
import com.appmelt.builder.ui.theme.TextSecondary
import com.appmelt.builder.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: MainViewModel,
    project: Project,
    analysis: ProjectAnalysis?,
    selectedVariant: BuildVariant,
    onVariantSelect: (BuildVariant) -> Unit,
    onStartBuild: () -> Unit,
    onSimulateError: (String) -> Unit,
    onPreBuildAiAudit: () -> Unit,
    onOpenFile: (File) -> Unit,
    onManageKeystores: () -> Unit,
    onBack: () -> Unit
) {
    val projectFiles = remember(project.path) {
        viewModel.projectManager.getProjectFiles(File(project.path))
    }
    val keystores by viewModel.keystores.collectAsState()
    val releaseKey = keystores.firstOrNull { !it.isDebug }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = project.packageId,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onPreBuildAiAudit, modifier = Modifier.testTag("detail_ai_audit_button")) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Audit",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            // Project Overview Card
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
                                text = "Project Analysis",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            StatusBadge(status = project.lastBuildStatus)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatItem(label = "compileSdk", value = "${project.compileSdk}", modifier = Modifier.weight(1f))
                            StatItem(label = "minSdk", value = "${project.minSdk}", modifier = Modifier.weight(1f))
                            StatItem(label = "AGP Version", value = project.agpVersion, modifier = Modifier.weight(1f))
                            StatItem(label = "JDK Runtime", value = "Java ${project.requiredJdk}", modifier = Modifier.weight(1f))
                        }

                        if (analysis?.warnings?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            analysis.warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Project Dedicated Keystore & Signing Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Project Keystore & Signing",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (releaseKey != null) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (releaseKey != null) "Release Key Ready" else "Debug Key Only",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (releaseKey != null) SuccessGreen else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (releaseKey != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = releaseKey.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Key Alias: ${releaseKey.keyAlias}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "This project is currently using the project debug keystore. Configure a dedicated release keystore or import key.properties below for release builds.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onManageKeystores,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_manage_keys_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Keystores for ${project.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Build Configuration Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Build Output Configuration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BuildVariant.values().forEach { variant ->
                                val isSelected = selectedVariant == variant
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onVariantSelect(variant) },
                                    label = { Text(variant.displayName, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("variant_chip_${variant.name}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary Build Button
                        Button(
                            onClick = onStartBuild,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_build_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Build ${selectedVariant.displayName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Testing & Error simulation triggers (to test Error Translation Layer)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSimulateError("missing_sdk") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_missing_sdk_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Missing SDK Error", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onSimulateError("dependency_error") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_dependency_error_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Offline Sync Error", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Project Files Section (Lightweight Editor Integration)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project Files & Configs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap to open Lightweight Editor",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // List files
            items(projectFiles.take(15)) { file ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(file) }
                        .testTag("file_item_${file.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Code,
                                contentDescription = null,
                                tint = if (file.name.endsWith(".kts") || file.name.endsWith(".gradle")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = file.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = file.parentFile?.name ?: "",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = if (file.isFile) "${file.length() / 1024} KB" else "dir",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
