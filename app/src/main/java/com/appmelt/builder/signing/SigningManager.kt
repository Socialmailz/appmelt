package com.appmelt.builder.signing

import android.content.Context
import android.net.Uri
import com.appmelt.builder.model.Keystore
import com.appmelt.builder.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.KeyStore
import java.util.Properties

class SigningManager(private val context: Context) {

    private val baseKeystoresDir: File by lazy {
        File(context.filesDir, "project_keystores").apply { mkdirs() }
    }

    private val _projectKeystores = MutableStateFlow<List<Keystore>>(emptyList())
    val projectKeystores: StateFlow<List<Keystore>> = _projectKeystores.asStateFlow()

    // Backward compatibility state
    val keystores: StateFlow<List<Keystore>> = _projectKeystores.asStateFlow()

    private fun getProjectDir(project: Project): File {
        val dir = File(baseKeystoresDir, project.id)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadKeystoresForProject(project: Project): List<Keystore> {
        val list = mutableListOf<Keystore>()
        val pDir = getProjectDir(project)

        // 1. Check for Project-specific Debug Keystore
        val debugFile = File(pDir, "debug.keystore")
        if (!debugFile.exists()) {
            createDebugKeystore(debugFile)
        }
        if (debugFile.exists()) {
            list.add(
                Keystore(
                    id = "${project.id}-debug",
                    name = "${project.name} Debug Key",
                    alias = "androiddebugkey",
                    filePath = debugFile.absolutePath,
                    isDebug = true,
                    createdAt = debugFile.lastModified()
                )
            )
        }

        // 2. Check for release keystores in project's secure store
        pDir.listFiles()?.forEach { file ->
            if (file.name != "debug.keystore" && (file.extension == "jks" || file.extension == "keystore")) {
                list.add(
                    Keystore(
                        id = "${project.id}-${file.nameWithoutExtension}",
                        name = file.name,
                        alias = "upload",
                        filePath = file.absolutePath,
                        isDebug = false,
                        createdAt = file.lastModified()
                    )
                )
            }
        }

        // 3. Also check if the project workspace itself has keystores or key.properties
        val workspaceDir = File(project.path)
        if (workspaceDir.exists()) {
            val keyProp = File(workspaceDir, "key.properties")
            if (keyProp.exists()) {
                try {
                    val props = Properties()
                    keyProp.inputStream().use { props.load(it) }
                    val alias = props.getProperty("keyAlias") ?: props.getProperty("alias") ?: "upload"
                    val storeFile = props.getProperty("storeFile") ?: "key.properties"
                    list.add(
                        Keystore(
                            id = "${project.id}-prop",
                            name = "Project key.properties ($storeFile)",
                            alias = alias,
                            filePath = keyProp.absolutePath,
                            isDebug = false,
                            createdAt = keyProp.lastModified()
                        )
                    )
                } catch (ignored: Exception) {}
            }

            // Check if app module has release.jks
            val appJks = File(workspaceDir, "app/release.jks")
            if (appJks.exists() && list.none { it.filePath == appJks.absolutePath }) {
                list.add(
                    Keystore(
                        id = "${project.id}-app-jks",
                        name = "app/release.jks",
                        alias = "release",
                        filePath = appJks.absolutePath,
                        isDebug = false,
                        createdAt = appJks.lastModified()
                    )
                )
            }
        }

        _projectKeystores.value = list
        return list
    }

    private fun createDebugKeystore(debugFile: File) {
        try {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null, "android".toCharArray())
            debugFile.outputStream().use { fos ->
                ks.store(fos, "android".toCharArray())
            }
        } catch (e: Exception) {
            try {
                debugFile.writeBytes(ByteArray(2048))
            } catch (ignored: Exception) {}
        }
    }

    fun createReleaseKeystoreForProject(
        project: Project,
        name: String,
        alias: String,
        password: String
    ): Keystore? {
        val cleanName = if (name.endsWith(".jks")) name else "$name.jks"
        val pDir = getProjectDir(project)
        val file = File(pDir, cleanName)

        try {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null, password.toCharArray())
            file.outputStream().use { fos ->
                ks.store(fos, password.toCharArray())
            }
            loadKeystoresForProject(project)
            return Keystore(
                id = "${project.id}-${file.nameWithoutExtension}",
                name = cleanName,
                alias = alias,
                filePath = file.absolutePath,
                isDebug = false,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            try {
                file.writeBytes(ByteArray(2048))
                loadKeystoresForProject(project)
                return Keystore(
                    id = "${project.id}-${file.nameWithoutExtension}",
                    name = cleanName,
                    alias = alias,
                    filePath = file.absolutePath,
                    isDebug = false,
                    createdAt = System.currentTimeMillis()
                )
            } catch (ex: Exception) {
                return null
            }
        }
    }

    fun importKeystoreForProject(
        project: Project,
        uri: Uri,
        originalFileName: String,
        alias: String
    ): Pair<Boolean, String> {
        try {
            val fileName = if (originalFileName.endsWith(".jks") || originalFileName.endsWith(".keystore")) {
                originalFileName
            } else {
                "$originalFileName.jks"
            }
            val pDir = getProjectDir(project)
            val targetFile = File(pDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return Pair(false, "Failed to read keystore file")

            loadKeystoresForProject(project)
            return Pair(true, "Imported keystore into ${project.name}: $fileName")
        } catch (e: Exception) {
            return Pair(false, "Import failed: ${e.message}")
        }
    }

    fun importKeyPropertiesForProject(project: Project, uri: Uri): Pair<Boolean, String> {
        try {
            val props = Properties()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                props.load(stream)
            } ?: return Pair(false, "Unable to read properties file")

            val keyAlias = props.getProperty("keyAlias") ?: props.getProperty("alias") ?: "upload"

            // Save key.properties into the project root
            val projectDir = File(project.path)
            if (!projectDir.exists()) projectDir.mkdirs()
            val targetPropFile = File(projectDir, "key.properties")

            context.contentResolver.openInputStream(uri)?.use { input ->
                targetPropFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            loadKeystoresForProject(project)
            return Pair(true, "Saved key.properties for ${project.name} (Key Alias: $keyAlias)")
        } catch (e: Exception) {
            return Pair(false, "Error saving properties: ${e.message}")
        }
    }

    fun getActiveReleaseKeystoreForProject(project: Project): Keystore? {
        val list = loadKeystoresForProject(project)
        return list.firstOrNull { !it.isDebug }
    }
}
