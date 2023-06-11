package dev.redicloud.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun zipFile(sourceFilePath: String, zipFilePath: String) {
    val sourceFile = File(sourceFilePath)
    val zipFile = File(zipFilePath)

    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
        if (sourceFile.isFile) {
            zipFile(sourceFile, sourceFile.name, zipOut)
        } else if (sourceFile.isDirectory) {
            sourceFile.listFiles()?.forEach { file ->
                zipFile(file, file.name, zipOut)
            }
        }
    }
}

private fun zipFile(file: File, fileName: String, zipOut: ZipOutputStream) {
    FileInputStream(file).use { fileIn ->
        BufferedInputStream(fileIn).use { bufferedIn ->
            val entry = ZipEntry(fileName)
            zipOut.putNextEntry(entry)

            bufferedIn.copyTo(zipOut, DEFAULT_BUFFER_SIZE)

            zipOut.closeEntry()
        }
    }
}

fun unzipFile(zipFilePath: String, destinationFolderPath: String) {
    val zipFile = File(zipFilePath)
    val destinationFolder = File(destinationFolderPath)

    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
        var entry = zipIn.nextEntry

        while (entry != null) {
            val entryFile = File(destinationFolder, entry.name)
            if (entry.isDirectory) {
                entryFile.mkdirs()
            } else {
                entryFile.parentFile?.mkdirs()

                FileOutputStream(entryFile).use { fileOut ->
                    BufferedOutputStream(fileOut).use { bufferedOut ->
                        zipIn.copyTo(bufferedOut, DEFAULT_BUFFER_SIZE)
                    }
                }
            }

            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
    }
}