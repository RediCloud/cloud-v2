package dev.redicloud.utils

import java.io.File
import java.nio.file.Paths

private val cloudPath: String = System.getProperty("redicloud.cloud.path") ?: Paths.get("").toAbsolutePath().toString()

val NODE_JSON = CloudFile("node.json")
val TEMP_FOLDER = CloudFile("tmp", folder = true)
val TEMP_SERVER_VERSION_FOLDER = CloudFile("server-version", "tmp", folder = true)
val STATIC_FOLDER = CloudFile("static", folder = true)
val STORAGE_FOLDER = CloudFile("storage", folder = true)
val LOG_FOLDER = CloudFile("logs", "storage", folder = true)
val CONSOLE_HISTORY_FILE = CloudFile(".console.history", "storage/logs")
val MINECRAFT_VERSIONS_FOLDER = CloudFile("versions", "storage", folder = true)
val TEMPLATE_FOLDER = CloudFile("templates", "storage", folder = true)
val DATABASE_JSON = CloudFile("database.json", "storage")

class CloudFile(val name: String, val parent: String = "", val folder: Boolean = false) {
    fun getFile(): File {
        val path = if (parent.isEmpty()) {
            cloudPath + File.separator + name
        }else {
            cloudPath + File.separator + parent + File.separator + name
        }
        return File(path)
    }

    fun createIfNotExists(): File {
        val file = getFile()
        if (file.exists()) return file
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        if (folder) {
            file.mkdir()
        }else {
            file.createNewFile()
        }
        return file
    }

    fun create(): File {
        val file = getFile()
        if (file.exists()) {
            file.delete()
        }
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        if (folder) {
            file.mkdir()
        }else {
            file.createNewFile()
        }
        return file
    }

}
