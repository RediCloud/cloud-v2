package dev.redicloud.utils

import java.io.File
import java.util.Properties
import java.util.jar.JarEntry
import java.util.jar.JarFile

class JarView(
    val file: File
) {

    init {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist")
        }
        if (!file.isFile || file.extension != "jar") {
            throw IllegalArgumentException("File is not a file")
        }
    }

    private val jarFile = JarFile(file)
    private val jarEntries = jarFile.entries()

    fun getEntries(): List<JarEntry> = jarEntries.toList()

    fun hasEntry(name: String): Boolean = jarFile.getJarEntry(name) != null

    fun getEntry(name: String): JarEntry? = jarFile.getJarEntry(name)

    fun getProperty(name: String): Properties? {
        val entry = getEntry(name) ?: return null
        val properties = Properties()
        jarFile.getInputStream(entry).use {
            properties.load(it)
        }
        return properties
    }

    fun close() {
        jarFile.close()
    }

}