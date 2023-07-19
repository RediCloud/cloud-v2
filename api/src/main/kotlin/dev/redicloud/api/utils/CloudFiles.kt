package dev.redicloud.api.utils

import java.io.File
import java.nio.file.Paths

val CLOUD_PATH: String = System.getenv()["RC_PATH"] ?: Paths.get("").toAbsolutePath().toString()

val NODE_JSON = CloudFile("node.json")
val LIB_FOLDER = CloudFile(".libs", folder = true)
val TEMP_FOLDER = CloudFile("tmp", folder = true)
val TEMP_SERVER_VERSION_FOLDER = CloudFile("server-version", "tmp", folder = true)
val TEMP_SERVER_FOLDER = CloudFile("server", "tmp", folder = true)
val TEMP_FILE_TRANSFER_FOLDER = CloudFile("file-transfer", "tmp", folder = true)
val STATIC_FOLDER = CloudFile("static", "storage", folder = true)
val STORAGE_FOLDER = CloudFile("storage", folder = true)
val LOG_FOLDER = CloudFile("logs", "storage", folder = true)
val CONSOLE_HISTORY_FILE = CloudFile(".console.history", "storage/logs")
val MINECRAFT_VERSIONS_FOLDER = CloudFile("versions", "storage", folder = true)
val TEMPLATE_FOLDER = CloudFile("templates", "storage", folder = true)
val DATABASE_JSON = CloudFile("database.json", "storage")
val CONNECTORS_FOLDER = CloudFile("connectors", "storage", folder = true)
val MODULE_FOLDER = CloudFile("modules", "storage", folder = true)

fun toCloudFile(universalPath: String): File {
    return File(CLOUD_PATH, universalPath)
}

fun toUniversalPath(file: File): String {
    val path = file.absolutePath.replace(CLOUD_PATH, "")
    return if (path.startsWith(File.separator)) path.replaceFirst(File.separator, "") else path
}

class CloudFile(val name: String, val parent: String = "", val folder: Boolean = false) {

    fun getCloudPath(): String {
        return if (parent.isEmpty()) {
            CLOUD_PATH + File.separator + name
        }else {
            CLOUD_PATH + File.separator + parent + File.separator + name
        }
    }

    fun getFile(): File {
        return File(getCloudPath())
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
