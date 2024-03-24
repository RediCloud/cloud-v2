package cloud

import dev.redicloud.api.utils.DATABASE_JSON
import dev.redicloud.api.utils.MINECRAFT_VERSIONS_FOLDER
import dev.redicloud.api.utils.NODE_JSON
import dev.redicloud.api.utils.STORAGE_FOLDER
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.database.config.DatabaseNode
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.toUUID
import redis.RedisInstance
import java.io.File

class CloudFileCopier(
    private val workingDirectory: File
) {

    val nodeJar: File = findNodeJar() ?: throw IllegalStateException("Node jar not found in working directory")
    val startFile: File = findStartFile() ?: throw IllegalStateException("Start file not found in working directory")

    init {
        STORAGE_FOLDER.createIfNotExists()
        MINECRAFT_VERSIONS_FOLDER.createIfNotExists()
        DATABASE_JSON.createIfNotExists()
        NODE_JSON.createIfNotExists()
    }

    fun createDatabaseFile(redis: RedisInstance) {
        val databaseFile = DATABASE_JSON.getFile()
        val configuration = DatabaseConfiguration(
            null,
            "",
            mutableListOf(
                DatabaseNode("127.0.0.1", redis.port)
            )
        )
        databaseFile.writeText(gson.toJson(configuration))
    }

    fun createNodeFile(nodeName: String, cloudName: String) {
        val nodeFile = NODE_JSON.getFile()
        val configuration = NodeConfiguration(
            nodeName,
            "${cloudName}_$nodeName".toUUID(),
            "127.0.0.1"
        )
        nodeFile.writeText(gson.toJson(configuration))
    }

    private fun findNodeJar(): File? {
        return workingDirectory.listFiles()
            ?.filter { it.isFile }
            ?.filter { it.extension == "jar" }
            ?.find { it.name.contains("node") }
    }

    private fun findStartFile(): File? {
        return workingDirectory.listFiles()
            ?.filter { it.isFile }
            ?.find { it.name.contains("start") }
    }

}