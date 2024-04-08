package dev.redicloud.testing.utils

import java.io.File

data class FileSelect(
    var file: File,
    var targetDirectory: String = ""
)

data class ProjectFileSelect(
    var projectName: String,
    var fileName: String? = null,
    var targetDirectory: String = "",
    var selectStrategy: FileSelectStrategy = FileSelectStrategy.LATEST_MODIFIED,
    var shadowJar: Boolean = false
)