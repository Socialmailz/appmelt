package com.appmelt.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.appmelt.builder.ui.editor.LightweightEditorScreen
import com.appmelt.builder.ui.screens.AIAssistantScreen
import com.appmelt.builder.ui.screens.BuildLogsScreen
import com.appmelt.builder.ui.screens.BuildProgressScreen
import com.appmelt.builder.ui.screens.BuildResultScreen
import com.appmelt.builder.ui.screens.HomeScreen
import com.appmelt.builder.ui.screens.KeystoresScreen
import com.appmelt.builder.ui.screens.ProjectDetailScreen
import com.appmelt.builder.ui.screens.RuntimesScreen
import com.appmelt.builder.ui.theme.AppMeltTheme
import com.appmelt.builder.viewmodel.MainViewModel
import com.appmelt.builder.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppMeltTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppMeltApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppMeltApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedAnalysis by viewModel.selectedProjectAnalysis.collectAsState()
    val selectedVariant by viewModel.selectedVariant.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val buildProgressPercent by viewModel.buildProgressPercent.collectAsState()
    val currentStepTitle by viewModel.currentStepTitle.collectAsState()
    val buildSteps by viewModel.buildSteps.collectAsState()
    val buildLogs by viewModel.buildLogs.collectAsState()
    val lastBuildResult by viewModel.lastBuildResult.collectAsState()
    val runtimeComponents by viewModel.runtimeComponents.collectAsState()
    val keystores by viewModel.keystores.collectAsState()
    val aiMessages by viewModel.aiMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val editingFile by viewModel.editingFile.collectAsState()
    val editingContent by viewModel.editingFileContent.collectAsState()

    // Global back handling
    BackHandler(enabled = currentScreen != Screen.PROJECTS) {
        when (currentScreen) {
            Screen.PROJECT_DETAIL -> viewModel.navigateTo(Screen.PROJECTS)
            Screen.BUILD_PROGRESS -> {
                if (!isBuilding) viewModel.navigateTo(Screen.PROJECT_DETAIL) else viewModel.navigateTo(Screen.PROJECTS)
            }
            Screen.BUILD_RESULT -> viewModel.navigateTo(Screen.PROJECT_DETAIL)
            Screen.BUILD_LOGS -> viewModel.navigateTo(Screen.BUILD_PROGRESS)
            Screen.RUNTIMES -> viewModel.navigateTo(Screen.PROJECTS)
            Screen.KEYSTORES -> {
                if (selectedProject != null) viewModel.navigateTo(Screen.PROJECT_DETAIL) else viewModel.navigateTo(Screen.PROJECTS)
            }
            Screen.AI_ASSISTANT -> {
                if (selectedProject != null) viewModel.navigateTo(Screen.PROJECT_DETAIL) else viewModel.navigateTo(Screen.PROJECTS)
            }
            Screen.CODE_EDITOR -> viewModel.navigateTo(Screen.PROJECT_DETAIL)
            else -> viewModel.navigateTo(Screen.PROJECTS)
        }
    }

    when (currentScreen) {
        Screen.PROJECTS -> {
            HomeScreen(
                viewModel = viewModel,
                projects = projects,
                onProjectClick = { viewModel.selectProject(it) },
                onRuntimesClick = { viewModel.navigateTo(Screen.RUNTIMES) },
                onAiClick = { viewModel.navigateTo(Screen.AI_ASSISTANT) }
            )
        }
        Screen.PROJECT_DETAIL -> {
            selectedProject?.let { project ->
                ProjectDetailScreen(
                    viewModel = viewModel,
                    project = project,
                    analysis = selectedAnalysis,
                    selectedVariant = selectedVariant,
                    onVariantSelect = { viewModel.selectBuildVariant(it) },
                    onStartBuild = { viewModel.startBuild() },
                    onSimulateError = { errorType -> viewModel.startBuild(simulateError = errorType) },
                    onPreBuildAiAudit = { viewModel.triggerAiPreBuildCheck() },
                    onOpenFile = { file -> viewModel.openFileForEditing(file) },
                    onManageKeystores = { viewModel.navigateTo(Screen.KEYSTORES) },
                    onBack = { viewModel.navigateTo(Screen.PROJECTS) }
                )
            } ?: run {
                viewModel.navigateTo(Screen.PROJECTS)
            }
        }
        Screen.BUILD_PROGRESS -> {
            BuildProgressScreen(
                viewModel = viewModel,
                project = selectedProject,
                isBuilding = isBuilding,
                progressPercent = buildProgressPercent,
                currentStepTitle = currentStepTitle,
                steps = buildSteps,
                logs = buildLogs,
                lastResult = lastBuildResult,
                onViewLogs = { viewModel.navigateTo(Screen.BUILD_LOGS) },
                onBack = {
                    if (selectedProject != null) viewModel.navigateTo(Screen.PROJECT_DETAIL) else viewModel.navigateTo(Screen.PROJECTS)
                }
            )
        }
        Screen.BUILD_RESULT -> {
            BuildResultScreen(
                viewModel = viewModel,
                project = selectedProject,
                result = lastBuildResult,
                onViewLogs = { viewModel.navigateTo(Screen.BUILD_LOGS) },
                onBackToProject = { viewModel.navigateTo(Screen.PROJECT_DETAIL) }
            )
        }
        Screen.BUILD_LOGS -> {
            BuildLogsScreen(
                logs = buildLogs,
                onBack = { viewModel.navigateTo(Screen.BUILD_PROGRESS) }
            )
        }
        Screen.RUNTIMES -> {
            RuntimesScreen(
                viewModel = viewModel,
                components = runtimeComponents,
                onBack = { viewModel.navigateTo(Screen.PROJECTS) }
            )
        }
        Screen.KEYSTORES -> {
            KeystoresScreen(
                viewModel = viewModel,
                project = selectedProject,
                keystores = keystores,
                onBack = {
                    if (selectedProject != null) viewModel.navigateTo(Screen.PROJECT_DETAIL) else viewModel.navigateTo(Screen.PROJECTS)
                }
            )
        }
        Screen.AI_ASSISTANT -> {
            AIAssistantScreen(
                viewModel = viewModel,
                project = selectedProject,
                messages = aiMessages,
                isThinking = isAiThinking,
                onBack = {
                    if (selectedProject != null) viewModel.navigateTo(Screen.PROJECT_DETAIL) else viewModel.navigateTo(Screen.PROJECTS)
                }
            )
        }
        Screen.CODE_EDITOR -> {
            editingFile?.let { file ->
                LightweightEditorScreen(
                    file = file,
                    initialContent = editingContent,
                    onSave = { newContent -> viewModel.saveEditingFile(newContent) },
                    onBack = { viewModel.navigateTo(Screen.PROJECT_DETAIL) }
                )
            } ?: run {
                viewModel.navigateTo(Screen.PROJECT_DETAIL)
            }
        }
    }
}
