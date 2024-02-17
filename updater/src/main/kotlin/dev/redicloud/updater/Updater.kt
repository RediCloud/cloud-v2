package dev.redicloud.updater

import java.io.File
import java.util.zip.ZipInputStream

object Updater {

    fun findNewestNodeJar(): File {
        val currentDirectory = File(".")
        val files = currentDirectory.listFiles()!!
            .filter { it.isFile }.filter { it.extension == "jar" }
        if (files.isEmpty()) {
            throw IllegalStateException("No jar files found in the current directory")
        }
        val jarMap = mutableMapOf<String, String>()
        files.forEach { file ->
            val inputStream = ZipInputStream(file.inputStream())
            var entry = inputStream.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    entry = inputStream.nextEntry
                    continue
                }
                if (entry.name != "redicloud-version.properties") {
                    entry = inputStream.nextEntry
                    continue
                }
                val properties = inputStream.bufferedReader().readLines()
                val line = properties.firstOrNull { it.startsWith("build_number=") }
                if (line != null) {
                    val buildNumber = line.split("=")[1]
                    jarMap[file.name] = buildNumber
                }
            }
        }
        var jar = ""
        var buildNumber = -1
        jarMap.forEach { (key, value) ->
            if (value == "latest" || value == "local") {
                return File(jar)
            }
            val number = value.toInt()
            if (number > buildNumber) {
                jar = key
                buildNumber = number
            }
        }
        return File(jar)
    }

    fun downloadLatest(): File {
        TODO()
    }

    fun updateAvailable(): Boolean {
        TODO()
    }

}