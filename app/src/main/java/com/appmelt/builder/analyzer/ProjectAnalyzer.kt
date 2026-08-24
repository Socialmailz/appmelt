package com.appmelt.builder.analyzer

import com.appmelt.builder.model.BuildVariant
import com.appmelt.builder.model.ProjectAnalysis
import java.io.File
import java.util.Properties

object ProjectAnalyzer {

    fun analyzeProject(projectDir: File): ProjectAnalysis {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return ProjectAnalysis(
                isValidAndroidProject = false,
                summary = "Project directory does not exist or is invalid."
            )
        }

        val hasSettingsGradleKts = File(projectDir, "settings.gradle.kts").exists()
        val hasSettingsGradle = File(projectDir, "settings.gradle").exists()
        val hasRootBuildKts = File(projectDir, "build.gradle.kts").exists()
        val hasRootBuild = File(projectDir, "build.gradle").exists()
        
        val appModuleDir = File(projectDir, "app")
        val hasAppBuildKts = File(appModuleDir, "build.gradle.kts").exists()
        val hasAppBuild = File(appModuleDir, "build.gradle").exists()

        val wrapperPropsFile = File(projectDir, "gradle/wrapper/gradle-wrapper.properties")
        val hasGradleWrapper = wrapperPropsFile.exists()

        if (!hasSettingsGradle && !hasSettingsGradleKts && !hasRootBuild && !hasRootBuildKts && !hasAppBuildKts && !hasAppBuild) {
            return ProjectAnalysis(
                isValidAndroidProject = false,
                summary = "No Gradle build scripts found in the selected folder."
            )
        }

        // Determine Project Name
        var projectName = projectDir.name
        val settingsFile = if (hasSettingsGradleKts) File(projectDir, "settings.gradle.kts") else if (hasSettingsGradle) File(projectDir, "settings.gradle") else null
        if (settingsFile != null && settingsFile.exists()) {
            val content = settingsFile.readText()
            val match = Regex("""rootProject\.name\s*=\s*["']([^"']+)["']""").find(content)
            if (match != null) {
                projectName = match.groupValues[1]
            }
        }

        // Parse Gradle Version from wrapper
        var gradleVersion = "8.11"
        if (hasGradleWrapper) {
            try {
                val props = Properties()
                wrapperPropsFile.inputStream().use { props.load(it) }
                val distUrl = props.getProperty("distributionUrl", "")
                val gvMatch = Regex("""gradle-([0-9.]+)-""").find(distUrl)
                if (gvMatch != null) {
                    gradleVersion = gvMatch.groupValues[1]
                }
            } catch (e: Exception) {
                // Keep default
            }
        }

        // Parse Version Catalog libs.versions.toml if present
        val tomlFile = File(projectDir, "gradle/libs.versions.toml")
        var agpVersion = "9.1.1"
        var kotlinVersion = "2.2.10"
        if (tomlFile.exists()) {
            val tomlContent = tomlFile.readText()
            Regex("""agp\s*=\s*["']([^"']+)["']""").find(tomlContent)?.let {
                agpVersion = it.groupValues[1]
            }
            Regex("""kotlin\s*=\s*["']([^"']+)["']""").find(tomlContent)?.let {
                kotlinVersion = it.groupValues[1]
            }
        }

        // Parse App build.gradle / build.gradle.kts
        val appBuildFile = if (hasAppBuildKts) File(appModuleDir, "build.gradle.kts") 
            else if (hasAppBuild) File(appModuleDir, "build.gradle")
            else if (hasRootBuildKts) File(projectDir, "build.gradle.kts")
            else if (hasRootBuild) File(projectDir, "build.gradle")
            else null

        var compileSdk = 36
        var minSdk = 24
        var targetSdk = 36
        var packageId = "com.example.app"
        var hasReleaseSigning = false

        if (appBuildFile != null && appBuildFile.exists()) {
            val content = appBuildFile.readText()

            // compileSdk extraction
            Regex("""compileSdk\s*=\s*(\d+)""").find(content)?.let {
                compileSdk = it.groupValues[1].toIntOrNull() ?: 36
            } ?: Regex("""compileSdk\s*\{[^}]*release\((\d+)\)""").find(content)?.let {
                compileSdk = it.groupValues[1].toIntOrNull() ?: 36
            }

            // minSdk
            Regex("""minSdk\s*=\s*(\d+)""").find(content)?.let {
                minSdk = it.groupValues[1].toIntOrNull() ?: 24
            }

            // targetSdk
            Regex("""targetSdk\s*=\s*(\d+)""").find(content)?.let {
                targetSdk = it.groupValues[1].toIntOrNull() ?: 36
            }

            // applicationId / namespace
            Regex("""applicationId\s*=\s*["']([^"']+)["']""").find(content)?.let {
                packageId = it.groupValues[1]
            } ?: Regex("""namespace\s*=\s*["']([^"']+)["']""").find(content)?.let {
                packageId = it.groupValues[1]
            }

            // check release signing
            if (content.contains("signingConfigs") && content.contains("release")) {
                hasReleaseSigning = true
            }
        }

        // Check AndroidManifest.xml
        val manifestFile = File(projectDir, "app/src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            val manifestContent = manifestFile.readText()
            Regex("""package\s*=\s*["']([^"']+)["']""").find(manifestContent)?.let {
                if (packageId == "com.example.app") {
                    packageId = it.groupValues[1]
                }
            }
        }

        // Determine required JDK
        val requiredJdk = when {
            compileSdk >= 35 || agpVersion.startsWith("8") || agpVersion.startsWith("9") -> 17
            compileSdk >= 31 -> 17
            else -> 11
        }

        val warnings = mutableListOf<String>()
        if (!hasGradleWrapper) {
            warnings.add("Gradle Wrapper not found; AppMelt will provision compatible Gradle $gradleVersion.")
        }
        if (compileSdk > 36) {
            warnings.add("SDK $compileSdk is experimental; fallback to SDK 36 will be used.")
        }

        return ProjectAnalysis(
            isValidAndroidProject = true,
            hasSettingsGradle = hasSettingsGradle || hasSettingsGradleKts,
            hasBuildGradle = hasRootBuild || hasRootBuildKts || hasAppBuild || hasAppBuildKts,
            hasGradleWrapper = hasGradleWrapper,
            projectName = projectName,
            packageId = packageId,
            compileSdk = compileSdk,
            minSdk = minSdk,
            targetSdk = targetSdk,
            agpVersion = agpVersion,
            gradleVersion = gradleVersion,
            kotlinVersion = kotlinVersion,
            requiredJdk = requiredJdk,
            modules = listOf("app"),
            detectedBuildVariants = listOf(
                BuildVariant.DEBUG_APK,
                BuildVariant.RELEASE_APK,
                BuildVariant.RELEASE_AAB
            ),
            hasDebugSigning = true,
            hasReleaseSigning = hasReleaseSigning,
            missingRequirements = emptyList(),
            warnings = warnings,
            summary = "Android Gradle project detected: $projectName (compileSdk $compileSdk, JDK $requiredJdk, Gradle $gradleVersion)"
        )
    }
}
