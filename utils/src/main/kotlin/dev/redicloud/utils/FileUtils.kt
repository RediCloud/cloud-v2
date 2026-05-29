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

private const val BYTES_PER_MB = 1024 * 1024

fun toMb(bytes: Long): Long = bytes / BYTES_PER_MB
