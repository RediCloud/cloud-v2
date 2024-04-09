package dev.redicloud.testing.utils

import java.io.File

data class FileSelect(
    var file: File,
    var targetDirectory: String = "",
    var prefixDirectory: String = "%cloud%"
)

data class ProjectFileSelect(
    var projectName: String,
    var fileName: String? = null,
    var targetDirectory: String = "",
    var selectStrategy: FileSelectStrategy = FileSelectStrategy.LATEST_MODIFIED,
    var shadowJar: Boolean = false
) {

    val file: File
        get() {
            val project = File(projectName)
            if (!project.exists()) {
                throw IllegalArgumentException("Project $projectName does not exist")
            }
            val build = File(project, "build")
            if (!build.exists()) {
                throw IllegalArgumentException("Project $projectName does not have a build folder")
            }
            val libs = File(build, "libs")
            if (!libs.exists()) {
                throw IllegalArgumentException("Project $projectName does not have a libs folder")
            }
            val files =
                libs.listFiles() ?: throw IllegalArgumentException("Project $projectName does not have any files")
            val file = when (selectStrategy) {
                FileSelectStrategy.LATEST_MODIFIED -> files.filter {
                    if (fileName != null) {
                        it.name == fileName
                    } else {
                        true
                    }
                }.filter {
                    if (shadowJar) {
                        it.name.contains("all")
                    } else {
                        true
                    }
                }.maxByOrNull { it.lastModified() }
                FileSelectStrategy.OLDEST_MODIFIED -> files
                    .filter {
                        if (fileName != null) {
                            it.name == fileName
                        } else {
                            true
                        }
                    }.filter {
                        if (shadowJar) {
                            it.name.contains("all")
                        } else {
                            true
                        }
                    }.minByOrNull { it.lastModified() }
            } ?: throw IllegalArgumentException("Project $projectName does not have any files")
            return file
        }

}