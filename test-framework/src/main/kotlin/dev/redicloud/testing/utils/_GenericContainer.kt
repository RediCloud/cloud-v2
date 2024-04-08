package dev.redicloud.testing.utils

import org.testcontainers.containers.GenericContainer
import java.io.File

fun GenericContainer<*>.copyFolderContentFromContainer(containerPath: String, destinationPath: String) {
    // ls -p -1 -> -p adds a / to directories and -1 prints each file in a separate line
    execInContainer("ls", "-p", "-1", containerPath).stdout
        .lineSequence().filter { it.isNotBlank() }
        .forEach { fileName ->
            if (fileName.endsWith('/')) {
                val folderName = fileName.substringBeforeLast('/')
                File(destinationPath, folderName).mkdirs()
                copyFolderContentFromContainer("$containerPath/$folderName", "$destinationPath/$folderName")
            } else {
                File(destinationPath).mkdirs()
                copyFileFromContainer("$containerPath/$fileName", "$destinationPath/$fileName")
            }
        }
}