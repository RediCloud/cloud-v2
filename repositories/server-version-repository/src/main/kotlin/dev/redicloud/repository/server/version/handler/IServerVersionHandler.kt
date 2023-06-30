package dev.redicloud.repository.server.version.handler

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.MINECRAFT_VERSIONS_FOLDER
import java.io.File

interface IServerVersionHandler {

    val type: CloudServerVersionType
    var lastUpdateCheck: Long

    suspend fun download(version: CloudServerVersion, force: Boolean = false): File

    suspend fun canDownload(version: CloudServerVersion): Boolean

    fun isDownloaded(version: CloudServerVersion): Boolean = getJar(version).exists()

    suspend fun isUpdateAvailable(version: CloudServerVersion, force: Boolean = false): Boolean

    suspend fun getVersions(): List<ServerVersion>

    suspend fun getBuilds(version: ServerVersion): List<String>

    suspend fun update(version: CloudServerVersion): File

    suspend fun patch(version: CloudServerVersion)

    fun isPatched(version: CloudServerVersion): Boolean = version.libPattern != null && File(getFolder(version).absolutePath, ".patched").exists()

    fun isPatchVersion(version: CloudServerVersion): Boolean = version.libPattern != null

    fun getJar(version: CloudServerVersion): File = File(getFolder(version).absolutePath, "$version.name.jar")

    fun getFolder(version: CloudServerVersion): File = File(MINECRAFT_VERSIONS_FOLDER.getFile().absolutePath, version.name)

    companion object {
        private val CACHE = mutableMapOf<CloudServerVersionType, IServerVersionHandler>()

        fun getHandler(type: CloudServerVersionType): IServerVersionHandler = CACHE[type] ?: throw IllegalStateException("No handler for type $type")

        fun registerHandler(serverVersionRepository: ServerVersionRepository) {
            CloudServerVersionType.VALUES.forEach {
                if (CACHE.containsKey(it)) return@forEach
                val handler = when(it) {
                    CloudServerVersionType.VELOCITY, CloudServerVersionType.PAPER, CloudServerVersionType.FOLIA,
                    CloudServerVersionType.WATERFALL,
                    -> PaperMcServerVersionHandler(serverVersionRepository, it)
                    else -> {
                        ServerVersionHandler(it)
                    }
                }
                CACHE[it] = handler
            }
        }
    }

}