package dev.redicloud.service.node.repository.server.version.handler

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.MINECRAFT_VERSIONS_FOLDER
import java.io.File

interface IServerVersionHandler {

    val type: CloudServerVersionType

    suspend fun download(version: CloudServerVersion, force: Boolean): File

    fun isDownloaded(version: CloudServerVersion): Boolean = getJar(version).exists()

    suspend fun isUpdateAvailable(version: CloudServerVersion): Boolean

    suspend fun getVersions(): List<ServerVersion>

    suspend fun getBuilds(version: ServerVersion): List<String>

    suspend fun update(version: CloudServerVersion): File

    suspend fun patch(version: CloudServerVersion)

    fun isPatched(version: CloudServerVersion): Boolean = version.libPattern != null && File(".patched", getFolder(version).absolutePath).exists()

    fun isPatchVersion(version: CloudServerVersion): Boolean = version.libPattern != null

    fun getJar(version: CloudServerVersion): File = File("$version.name.jar", getFolder(version).absolutePath)

    fun getFolder(version: CloudServerVersion): File = File(version.name, MINECRAFT_VERSIONS_FOLDER.getFile().absolutePath)

    companion object {
        private val CACHE = mutableMapOf<CloudServerVersionType, IServerVersionHandler>()

        fun getHandler(type: CloudServerVersionType): IServerVersionHandler = CACHE[type]!!

        fun registerHandler(serverVersionRepository: ServerVersionRepository) {
            CloudServerVersionType.values().forEach {
                val handler = when(it) {
                    CloudServerVersionType.VELOCITY, CloudServerVersionType.PAPER, CloudServerVersionType.FOLIA,
                        CloudServerVersionType.WATERFALL,
                    -> PaperMcServerVersionHandler(serverVersionRepository, it)
                    else -> {
                        ServerVersionHandler(it)
                    }
                }
            }
        }
    }

}