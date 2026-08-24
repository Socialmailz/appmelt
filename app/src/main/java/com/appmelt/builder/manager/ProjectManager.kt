package com.appmelt.builder.manager

import android.content.Context
import android.net.Uri
import com.appmelt.builder.analyzer.ProjectAnalyzer
import com.appmelt.builder.model.BuildStatus
import com.appmelt.builder.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class ProjectManager(private val context: Context) {

    private val projectsDir: File by lazy {
        File(context.filesDir, "projects").apply { mkdirs() }
    }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    init {
        refreshProjects()
    }

    fun refreshProjects() {
        val list = mutableListOf<Project>()
        projectsDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val analysis = ProjectAnalyzer.analyzeProject(dir)
                val buildDir = File(context.filesDir, "builds")
                val possibleApk = File(buildDir, "${dir.name.lowercase()}-debug.apk")

                list.add(
                    Project(
                        id = dir.name,
                        name = analysis.projectName.ifEmpty { dir.name },
                        path = dir.absolutePath,
                        packageId = analysis.packageId,
                        versionName = "1.0",
                        versionCode = 1,
                        compileSdk = analysis.compileSdk,
                        minSdk = analysis.minSdk,
                        targetSdk = analysis.targetSdk,
                        agpVersion = analysis.agpVersion,
                        gradleVersion = analysis.gradleVersion,
                        kotlinVersion = analysis.kotlinVersion,
                        requiredJdk = analysis.requiredJdk,
                        lastBuildTime = if (possibleApk.exists()) possibleApk.lastModified() else dir.lastModified(),
                        lastBuildStatus = if (possibleApk.exists()) BuildStatus.SUCCESS else BuildStatus.READY,
                        lastGeneratedArtifactPath = if (possibleApk.exists()) possibleApk.absolutePath else null,
                        lastArtifactSizeFormatted = if (possibleApk.exists()) "${possibleApk.length() / (1024 * 1024)} MB" else null,
                        isSample = false
                    )
                )
            }
        }
        // Sort by last modified descending
        _projects.value = list.sortedByDescending { it.lastBuildTime }
    }

    suspend fun importProjectFromZip(zipUri: Uri, originalName: String): Project? = withContext(Dispatchers.IO) {
        try {
            val cleanName = originalName.removeSuffix(".zip").replace(" ", "_").lowercase()
            val targetDir = File(projectsDir, cleanName)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                unzip(inputStream, targetDir)
            }

            // If the zip contained a single nested root directory, locate it
            val effectiveDir = findRealProjectRoot(targetDir)

            val analysis = ProjectAnalyzer.analyzeProject(effectiveDir)
            refreshProjects()
            
            _projects.value.find { it.path == effectiveDir.absolutePath }
        } catch (e: Exception) {
            null
        }
    }

    private fun findRealProjectRoot(dir: File): File {
        if (File(dir, "settings.gradle.kts").exists() || 
            File(dir, "settings.gradle").exists() || 
            File(dir, "build.gradle.kts").exists() || 
            File(dir, "build.gradle").exists()) {
            return dir
        }
        val subDirs = dir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        if (subDirs.size == 1) {
            return findRealProjectRoot(subDirs[0])
        }
        return dir
    }

    private fun unzip(inputStream: InputStream, targetDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun deleteProject(project: Project): Boolean {
        return try {
            File(project.path).deleteRecursively()
            refreshProjects()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getProjectFiles(projectDir: File): List<File> {
        val list = mutableListOf<File>()
        if (projectDir.exists()) {
            projectDir.walkTopDown()
                .filter { !it.name.startsWith(".") && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
                .take(100)
                .forEach { list.add(it) }
        }
        return list
    }

    fun readFileContent(file: File): String {
        return if (file.exists() && file.isFile) {
            file.readText()
        } else {
            ""
        }
    }

    fun saveFileContent(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
}
