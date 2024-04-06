package cloud

import dev.redicloud.api.utils.*
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.database.config.DatabaseNode
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.toUUID
import redis.RedisInstance
import java.io.File

class CloudFileCopier(
    val workingDirectory: File,
    val version: String,
    val cloudName: String,
    val cloudNodeName: String
) {

    val nodeJar: File
        get() = findNodeJar() ?: throw IllegalStateException("Node jar not found in working directory")
    val startFile: File
        get() = findStartFile() ?: throw IllegalStateException("Start file not found in working directory")

    init {
        STORAGE_FOLDER.createIfNotExists(workingDirectory)
        MINECRAFT_VERSIONS_FOLDER.createIfNotExists(workingDirectory)
        DATABASE_JSON.createIfNotExists(workingDirectory)
        CONNECTORS_FOLDER.createIfNotExists(workingDirectory)
        NODE_JSON.createIfNotExists(workingDirectory)
        MODULES_FOLDER.createIfNotExists(workingDirectory)

        copyNodeJar()
        copyModuleJars()
        copyConnectorJars()
        copyStartFiles()
    }

    fun copyNodeJar() {
        NODE_JAR.getFile(version)!!.copyTo(File(workingDirectory, "redicloud-node-service-$version.jar"), true)
    }

    fun copyModuleJars() {
        val modulesPath = MODULES_FOLDER.getCloudPath(workingDirectory)
        PAPER_MC_MODULE_JAR.getFile(version)!!.copyTo(File(modulesPath, "papermc-updater.jar"), true)
    }

    fun copyConnectorJars() {
        val connectorPath = CONNECTORS_FOLDER.getCloudPath(workingDirectory)
        BUKKIT_CONNECTOR_JAR.getFile(version)!!.copyTo(File(connectorPath, "redicloud-bukkit-connector-$version-local-local.jar"), true)
        BUNGEE_CONNECTOR_JAR.getFile(version)!!.copyTo(File(connectorPath, "redicloud-bungeecord-connector-$version-local-local.jar"), true)
        VELOCITY_CONNECTOR_JAR.getFile(version)!!.copyTo(File(connectorPath, "redicloud-velocity-connector-$version-local-local.jar"), true)
        MINESTOM_CONNECTOR_JAR.getFile(version)!!.copyTo(File(connectorPath, "redicloud-minestom-connector-$version-local-local.jar"), true)
    }

    fun copyStartFiles() {
        val targetBat = File(workingDirectory, "start.bat")
        START_BAT.getFile()!!.copyTo(targetBat, true)
        targetBat.writeText(replacePlaceholders(targetBat.readText()))

        val targetSh = File(workingDirectory, "start.sh")
        START_SH.getFile()!!.copyTo(targetSh, true)
        targetSh.writeText(replacePlaceholders(targetSh.readText()))
    }

    fun replacePlaceholders(text: String): String {
        return text
            .replace("%node_jar%", nodeJar.name)
            .replace("%version%", version)
            .replace("%branch%", "local")
            .replace("%build%", "local")
            .replace("%cloud_name%", cloudName)
            .replace("%cloud_node_name%", cloudNodeName)
    }

    fun createDatabaseFile(redis: RedisInstance) {
        val databaseFile = DATABASE_JSON.getFile(workingDirectory)
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
        val nodeFile = NODE_JSON.getFile(workingDirectory)
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
            ?.filter { it.extension == "sh" && getOperatingSystemType() == OSType.LINUX || it.extension == "bat" && getOperatingSystemType() == OSType.WINDOWS }
            ?.find { it.name.contains("start") }
    }

}