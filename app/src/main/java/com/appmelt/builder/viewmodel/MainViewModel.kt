package com.appmelt.builder.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appmelt.builder.ai.AIAssistant
import com.appmelt.builder.analyzer.ProjectAnalyzer
import com.appmelt.builder.engine.BuildEngine
import com.appmelt.builder.environment.EnvironmentManager
import com.appmelt.builder.errortranslator.ErrorTranslator
import com.appmelt.builder.manager.ProjectManager
import com.appmelt.builder.model.AIChatMessage
import com.appmelt.builder.model.BuildLogItem
import com.appmelt.builder.model.BuildResult
import com.appmelt.builder.model.BuildStep
import com.appmelt.builder.model.BuildVariant
import com.appmelt.builder.model.ConfigFixSuggestion
import com.appmelt.builder.model.FriendlyError
import com.appmelt.builder.model.Keystore
import com.appmelt.builder.model.Project
import com.appmelt.builder.model.ProjectAnalysis
import com.appmelt.builder.model.RuntimeComponent
import com.appmelt.builder.output.OutputManager
import com.appmelt.builder.signing.SigningManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    PROJECTS,
    PROJECT_DETAIL,
    BUILD_PROGRESS,
    BUILD_RESULT,
    BUILD_LOGS,
    RUNTIMES,
    KEYSTORES,
    AI_ASSISTANT,
    CODE_EDITOR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val projectManager = ProjectManager(application)
    val environmentManager = EnvironmentManager(application)
    val signingManager = SigningManager(application)
    val outputManager = OutputManager(application)
    val buildEngine = BuildEngine(application, environmentManager, signingManager)
    val aiAssistant = AIAssistant()

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.PROJECTS)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Selected Project & Analysis
    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _selectedProjectAnalysis = MutableStateFlow<ProjectAnalysis?>(null)
    val selectedProjectAnalysis: StateFlow<ProjectAnalysis?> = _selectedProjectAnalysis.asStateFlow()

    // Selected Build Variant
    private val _selectedVariant = MutableStateFlow(BuildVariant.DEBUG_APK)
    val selectedVariant: StateFlow<BuildVariant> = _selectedVariant.asStateFlow()

    // Last Build Result
    private val _lastBuildResult = MutableStateFlow<BuildResult?>(null)
    val lastBuildResult: StateFlow<BuildResult?> = _lastBuildResult.asStateFlow()

    // Active Editor File
    private val _editingFile = MutableStateFlow<File?>(null)
    val editingFile: StateFlow<File?> = _editingFile.asStateFlow()

    private val _editingFileContent = MutableStateFlow("")
    val editingFileContent: StateFlow<String> = _editingFileContent.asStateFlow()

    // AI Assistant Chat Messages
    private val _aiMessages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AIChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    val projects: StateFlow<List<Project>> = projectManager.projects
    val runtimeComponents: StateFlow<List<RuntimeComponent>> = environmentManager.components
    val keystores: StateFlow<List<Keystore>> = signingManager.projectKeystores
    val isBuilding: StateFlow<Boolean> = buildEngine.isBuilding
    val buildProgressPercent: StateFlow<Int> = buildEngine.progressPercent
    val currentStepTitle: StateFlow<String> = buildEngine.currentStepTitle
    val buildSteps: StateFlow<List<BuildStep>> = buildEngine.steps
    val buildLogs: StateFlow<List<BuildLogItem>> = buildEngine.buildLogs

    init {
        // Default AI greeting message
        _aiMessages.value = listOf(
            AIChatMessage(
                id = "init",
                isUser = false,
                message = "Hello! I am your AppMelt Android Build Assistant.\nI can help diagnose build errors, verify your compileSdk & Gradle configurations, and suggest safe fixes."
            )
        )
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectProject(project: Project) {
        _selectedProject.value = project
        _selectedProjectAnalysis.value = ProjectAnalyzer.analyzeProject(File(project.path))
        signingManager.loadKeystoresForProject(project)
        navigateTo(Screen.PROJECT_DETAIL)
    }

    fun selectBuildVariant(variant: BuildVariant) {
        _selectedVariant.value = variant
    }

    fun importZip(uri: Uri, name: String) {
        viewModelScope.launch {
            val project = projectManager.importProjectFromZip(uri, name)
            if (project != null) {
                selectProject(project)
            }
        }
    }

    fun startBuild(simulateError: String? = null) {
        val project = _selectedProject.value ?: return
        navigateTo(Screen.BUILD_PROGRESS)

        viewModelScope.launch {
            val result = buildEngine.executeBuild(
                project = project,
                variant = _selectedVariant.value,
                simulateError = simulateError
            )
            _lastBuildResult.value = result
            projectManager.refreshProjects()

            if (result.isSuccess) {
                navigateTo(Screen.BUILD_RESULT)
            }
        }
    }

    fun openFileForEditing(file: File) {
        _editingFile.value = file
        _editingFileContent.value = projectManager.readFileContent(file)
        navigateTo(Screen.CODE_EDITOR)
    }

    fun saveEditingFile(newContent: String) {
        val file = _editingFile.value ?: return
        projectManager.saveFileContent(file, newContent)
        _editingFileContent.value = newContent
        // Re-analyze project if a build file was modified
        _selectedProject.value?.let { proj ->
            _selectedProjectAnalysis.value = ProjectAnalyzer.analyzeProject(File(proj.path))
        }
    }

    fun installRuntime(componentId: String) {
        viewModelScope.launch {
            environmentManager.installComponent(componentId)
        }
    }

    fun uninstallRuntime(componentId: String) {
        environmentManager.uninstallComponent(componentId)
    }

    fun cleanCache() {
        environmentManager.clearDependencyCache()
    }

    fun createKeystoreForSelectedProject(name: String, alias: String, pass: String): Keystore? {
        val proj = _selectedProject.value ?: return null
        return signingManager.createReleaseKeystoreForProject(proj, name, alias, pass)
    }

    fun importKeystoreForSelectedProject(uri: Uri, fileName: String, alias: String): Pair<Boolean, String> {
        val proj = _selectedProject.value ?: return Pair(false, "No project selected")
        return signingManager.importKeystoreForProject(proj, uri, fileName, alias)
    }

    fun importKeyPropertiesForSelectedProject(uri: Uri): Pair<Boolean, String> {
        val proj = _selectedProject.value ?: return Pair(false, "No project selected")
        return signingManager.importKeyPropertiesForProject(proj, uri)
    }

    fun refreshSelectedProjectKeystores() {
        val proj = _selectedProject.value ?: return
        signingManager.loadKeystoresForProject(proj)
    }

    fun askAiAssistant(userPrompt: String) {
        val current = _aiMessages.value.toMutableList()
        val userMsg = AIChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            isUser = true,
            message = userPrompt
        )
        current.add(userMsg)
        _aiMessages.value = current
        _isAiThinking.value = true

        viewModelScope.launch {
            val response = aiAssistant.askAssistant(userPrompt, _selectedProject.value)
            val updated = _aiMessages.value.toMutableList()
            updated.add(response)
            _aiMessages.value = updated
            _isAiThinking.value = false
        }
    }

    fun triggerAiDiagnosisForError(friendlyError: FriendlyError) {
        navigateTo(Screen.AI_ASSISTANT)
        val prompt = "Why did my build fail with: ${friendlyError.title}?"
        val current = _aiMessages.value.toMutableList()
        current.add(AIChatMessage(id = java.util.UUID.randomUUID().toString(), isUser = true, message = prompt))
        _aiMessages.value = current
        _isAiThinking.value = true

        viewModelScope.launch {
            val recentLogs = buildLogs.value.takeLast(20).joinToString("\n") { it.message }
            val diagnosis = aiAssistant.diagnoseBuildError(
                errorTitle = friendlyError.title,
                errorSummary = friendlyError.summary,
                recentLogs = recentLogs,
                project = _selectedProject.value
            )
            val updated = _aiMessages.value.toMutableList()
            updated.add(diagnosis)
            _aiMessages.value = updated
            _isAiThinking.value = false
        }
    }

    fun triggerAiPreBuildCheck() {
        val proj = _selectedProject.value ?: return
        val analysis = _selectedProjectAnalysis.value ?: return
        navigateTo(Screen.AI_ASSISTANT)

        _isAiThinking.value = true
        viewModelScope.launch {
            val audit = aiAssistant.runPreBuildCheck(proj, analysis)
            val updated = _aiMessages.value.toMutableList()
            updated.add(AIChatMessage(id = java.util.UUID.randomUUID().toString(), isUser = true, message = "Run pre-build audit on ${proj.name}"))
            updated.add(audit)
            _aiMessages.value = updated
            _isAiThinking.value = false
        }
    }

    fun applyAiConfigFix(fix: ConfigFixSuggestion) {
        val project = _selectedProject.value ?: return
        val targetFile = File(project.path, fix.filePath)
        if (targetFile.exists()) {
            val current = targetFile.readText()
            val replaced = current.replace(fix.originalSnippet, fix.replacementSnippet)
            targetFile.writeText(replaced)
            _selectedProjectAnalysis.value = ProjectAnalyzer.analyzeProject(File(project.path))
            
            val currentMsgs = _aiMessages.value.toMutableList()
            currentMsgs.add(
                AIChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    isUser = false,
                    message = "✓ Applied fix to `${fix.filePath}` successfully! Ready to re-build."
                )
            )
            _aiMessages.value = currentMsgs
        }
    }
}
