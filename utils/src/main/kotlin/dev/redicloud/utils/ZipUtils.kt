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
            zipSingleFile(sourceFile, sourceFile.name, zipOut)
        } else if (sourceFile.isDirectory) {
            zipDirectory(sourceFile, zipOut, sourceFile.name)
        }
    }
}

private fun zipSingleFile(file: File, fileName: String, zipOut: ZipOutputStream) {
    FileInputStream(file).use { fileIn ->
        BufferedInputStream(fileIn).use { bufferedIn ->
            val entry = ZipEntry(fileName)
            zipOut.putNextEntry(entry)

            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead = bufferedIn.read(buffer)

            while (bytesRead != -1) {
                zipOut.write(buffer, 0, bytesRead)
                bytesRead = bufferedIn.read(buffer)
            }

            zipOut.closeEntry()
        }
    }
}

private fun zipDirectory(directory: File, zipOut: ZipOutputStream, baseDir: String) {
    directory.listFiles()?.forEach { file ->
        val entryName = if (baseDir.isNotEmpty()) {
            baseDir + File.separator + file.name
        } else {
            file.name
        }

        if (file.isFile) {
            zipSingleFile(file, entryName, zipOut)
        } else if (file.isDirectory) {
            zipDirectory(file, zipOut, entryName)
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
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead = zipIn.read(buffer)

                    while (bytesRead != -1) {
                        fileOut.write(buffer, 0, bytesRead)
                        bytesRead = zipIn.read(buffer)
                    }
                }
            }

            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
    }
}