package com.appmelt.builder.errortranslator

import com.appmelt.builder.model.ErrorActionType
import com.appmelt.builder.model.FriendlyError

object ErrorTranslator {

    fun translate(rawLogs: String, rawError: String? = null): FriendlyError {
        val fullText = "$rawError\n$rawLogs"

        return when {
            // Missing Android SDK
            fullText.contains("Failed to find target with hash string 'android-36'", ignoreCase = true) ||
            fullText.contains("SDK location not found", ignoreCase = true) ||
            (fullText.contains("compileSdkVersion", ignoreCase = true) && fullText.contains("36")) ||
            fullText.contains("Android SDK 36 is missing", ignoreCase = true) -> {
                FriendlyError(
                    title = "Android SDK 36 Missing",
                    summary = "This project requires Android SDK Platform 36 (Android 16), which is not yet installed in your on-device runtime.",
                    detailedReason = "The project's compileSdk is set to 36. Without android-36 platform components, AAPT2 and D8 cannot resolve framework symbols.",
                    actionText = "Install SDK 36 & Rebuild",
                    actionType = ErrorActionType.INSTALL_SDK,
                    actionPayload = "sdk-36"
                )
            }

            fullText.contains("android-35", ignoreCase = true) && (fullText.contains("not found", ignoreCase = true) || fullText.contains("missing", ignoreCase = true)) -> {
                FriendlyError(
                    title = "Android SDK 35 Missing",
                    summary = "This project targets Android SDK Platform 35. Please install the required runtime platform.",
                    detailedReason = "The Gradle configuration specifies compileSdk 35.",
                    actionText = "Install SDK 35",
                    actionType = ErrorActionType.INSTALL_SDK,
                    actionPayload = "sdk-35"
                )
            }

            // Java / JDK incompatibility
            fullText.contains("Unsupported class file major version", ignoreCase = true) ||
            fullText.contains("source compatibility", ignoreCase = true) ||
            fullText.contains("compileJava", ignoreCase = true) && fullText.contains("version 17", ignoreCase = true) ||
            fullText.contains("requires Java 17", ignoreCase = true) -> {
                FriendlyError(
                    title = "JDK Version Incompatibility",
                    summary = "This project requires OpenJDK 17 or higher for AGP and Kotlin compiler compatibility.",
                    detailedReason = "Android Gradle Plugin 8.x/9.x and Kotlin 2.x require Java 17 bytecode compatibility.",
                    actionText = "Switch to OpenJDK 17",
                    actionType = ErrorActionType.SWITCH_JDK,
                    actionPayload = "jdk-17"
                )
            }

            // Dependency Resolution Failure
            fullText.contains("Could not resolve", ignoreCase = true) ||
            fullText.contains("Could not download", ignoreCase = true) ||
            fullText.contains("UnknownHostException", ignoreCase = true) ||
            fullText.contains("ConnectException", ignoreCase = true) -> {
                FriendlyError(
                    title = "Dependency Download Failed",
                    summary = "Could not download required Maven / Google dependencies. Please verify your internet connection.",
                    detailedReason = "Gradle requires online access during the first build to populate the local on-device Maven cache.",
                    actionText = "Retry Dependency Sync",
                    actionType = ErrorActionType.DOWNLOAD_DEPENDENCIES,
                    actionPayload = "cache-maven"
                )
            }

            // Signing issues
            fullText.contains("Keystore file not found", ignoreCase = true) ||
            fullText.contains("Password verification failed", ignoreCase = true) ||
            fullText.contains("Cannot find key with alias", ignoreCase = true) ||
            fullText.contains("signingConfig", ignoreCase = true) && fullText.contains("incomplete", ignoreCase = true) -> {
                FriendlyError(
                    title = "Release Signing Incomplete",
                    summary = "Release build failed because signing configuration or keystore credentials could not be verified.",
                    detailedReason = "A signed release APK requires a valid JKS keystore, key alias, and valid key password.",
                    actionText = "Configure Keystore & Signing",
                    actionType = ErrorActionType.FIX_SIGNING,
                    actionPayload = "signing"
                )
            }

            // Out of memory / Low memory
            fullText.contains("OutOfMemoryError", ignoreCase = true) ||
            fullText.contains("GC overhead limit exceeded", ignoreCase = true) ||
            fullText.contains("low memory", ignoreCase = true) -> {
                FriendlyError(
                    title = "Device Memory Limit Reached",
                    summary = "Build could not complete because the phone ran out of available memory during dexing / Kotlin compilation.",
                    detailedReason = "Compiling large Compose projects requires sufficient RAM. AppMelt can enable single-worker low-memory build mode.",
                    actionText = "Build in Low-Memory Mode",
                    actionType = ErrorActionType.EDIT_CONFIG,
                    actionPayload = "low-memory"
                )
            }

            // AAPT2 XML / Resource errors
            fullText.contains("AAPT2 process failed", ignoreCase = true) ||
            fullText.contains("error: resource", ignoreCase = true) ||
            fullText.contains("AAPT:", ignoreCase = true) -> {
                FriendlyError(
                    title = "Resource Processing Error (AAPT2)",
                    summary = "One or more XML resource files or drawables contain syntax errors or missing IDs.",
                    detailedReason = "AAPT2 could not compile the resources directory. Check strings.xml, layouts, or drawable XMLs.",
                    actionText = "Open Lightweight Editor",
                    actionType = ErrorActionType.EDIT_CONFIG,
                    actionPayload = "editor"
                )
            }

            // Generic / Fallback error
            else -> {
                FriendlyError(
                    title = "Build Execution Error",
                    summary = "A Gradle task failed during compilation. You can diagnose this using the AI Build Assistant.",
                    detailedReason = rawError ?: "Gradle task exited with non-zero status code.",
                    actionText = "Ask AI Assistant",
                    actionType = ErrorActionType.ASK_AI,
                    actionPayload = "ai-diagnose"
                )
            }
        }
    }
}
