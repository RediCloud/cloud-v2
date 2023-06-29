package dev.redicloud.utils

import java.io.File

fun isInFile(folder: File, file: File): Boolean {
    if (!folder.isDirectory) return false

    if (folder == file || folder == file.parentFile) return true

    val folders = folder.listFiles { f -> f.isDirectory } ?: return false

    for (subfolder in folders) {
        if (isInFile(subfolder, file)) {
            return true
        }
    }

    return false
}

fun toMb(bytes: Long): Long = bytes / (1024 * 1024)