package com.appmelt.builder.model

enum class BuildStatus {
    NOT_BUILT,
    ANALYZING,
    READY,
    BUILDING,
    SUCCESS,
    FAILED
}

enum class BuildVariant(val displayName: String, val extension: String) {
    DEBUG_APK("Debug APK", "apk"),
    RELEASE_APK("Release APK (Signed)", "apk"),
    RELEASE_AAB("Release App Bundle (AAB)", "aab")
}

data class Project(
    val id: String,
    val name: String,
    val path: String,
    val packageId: String = "com.example.app",
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val compileSdk: Int = 36,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val agpVersion: String = "9.1.1",
    val gradleVersion: String = "8.11",
    val kotlinVersion: String = "2.2.10",
    val requiredJdk: Int = 17,
    val lastBuildTime: Long = 0L,
    val lastBuildStatus: BuildStatus = BuildStatus.NOT_BUILT,
    val lastGeneratedArtifactPath: String? = null,
    val lastArtifactSizeFormatted: String? = null,
    val isSample: Boolean = false
)

data class ProjectAnalysis(
    val isValidAndroidProject: Boolean,
    val hasSettingsGradle: Boolean = false,
    val hasBuildGradle: Boolean = false,
    val hasGradleWrapper: Boolean = false,
    val projectName: String = "",
    val packageId: String = "",
    val compileSdk: Int = 36,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val agpVersion: String = "9.1.1",
    val gradleVersion: String = "8.11",
    val kotlinVersion: String = "2.2.10",
    val requiredJdk: Int = 17,
    val modules: List<String> = listOf("app"),
    val detectedBuildVariants: List<BuildVariant> = listOf(BuildVariant.DEBUG_APK, BuildVariant.RELEASE_APK, BuildVariant.RELEASE_AAB),
    val hasDebugSigning: Boolean = true,
    val hasReleaseSigning: Boolean = false,
    val missingRequirements: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val summary: String = ""
)

data class RuntimeComponent(
    val id: String,
    val name: String,
    val type: ComponentType,
    val version: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val isInstalled: Boolean,
    val isRequired: Boolean = false
)

enum class ComponentType {
    JDK,
    ANDROID_SDK,
    BUILD_TOOLS,
    GRADLE,
    DEPENDENCY_CACHE
}

data class BuildEnvironment(
    val installedJdks: List<String> = listOf("17"),
    val installedSdks: List<Int> = listOf(35, 36),
    val installedBuildTools: List<String> = listOf("35.0.0", "36.0.0"),
    val installedGradle: List<String> = listOf("8.11"),
    val totalCacheSizeBytes: Long = 480L * 1024L * 1024L,
    val totalStorageUsedBytes: Long = 850L * 1024L * 1024L
)

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

data class BuildLogItem(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

data class BuildStep(
    val id: String,
    val title: String,
    val description: String,
    val progressWeight: Float,
    val isCompleted: Boolean = false,
    val isRunning: Boolean = false,
    val isFailed: Boolean = false
)

data class BuildResult(
    val isSuccess: Boolean,
    val artifactPath: String? = null,
    val artifactName: String = "",
    val artifactType: BuildVariant = BuildVariant.DEBUG_APK,
    val artifactSizeBytes: Long = 0L,
    val artifactSizeFormatted: String = "",
    val sha256Checksum: String = "",
    val packageId: String = "",
    val versionName: String = "",
    val versionCode: Int = 1,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val compileSdk: Int = 36,
    val durationMs: Long = 0L,
    val logs: List<BuildLogItem> = emptyList(),
    val friendlyError: FriendlyError? = null
)

data class FriendlyError(
    val title: String,
    val summary: String,
    val detailedReason: String,
    val actionText: String? = null,
    val actionType: ErrorActionType = ErrorActionType.NONE,
    val actionPayload: String = ""
)

enum class ErrorActionType {
    NONE,
    INSTALL_SDK,
    SWITCH_JDK,
    DOWNLOAD_DEPENDENCIES,
    FIX_SIGNING,
    EDIT_CONFIG,
    ASK_AI
}

data class Keystore(
    val id: String,
    val name: String,
    val alias: String,
    val filePath: String,
    val isDebug: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val keyAlias: String get() = alias
}

data class AIChatMessage(
    val id: String,
    val isUser: Boolean,
    val message: String,
    val suggestedFix: ConfigFixSuggestion? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConfigFixSuggestion(
    val filePath: String,
    val description: String,
    val originalSnippet: String,
    val replacementSnippet: String
)
