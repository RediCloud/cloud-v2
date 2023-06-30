package dev.redicloud.repository.server.version.handler

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.handler.defaults.PaperMcServerVersionHandler
import dev.redicloud.repository.server.version.handler.defaults.URLServerVersionHandler
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.MINECRAFT_VERSIONS_FOLDER
import java.io.File

interface IServerVersionHandler {

    val name: String
    val serverVersionRepository: ServerVersionRepository
    var lastUpdateCheck: Long

    suspend fun download(version: CloudServerVersion, force: Boolean = false): File

    suspend fun canDownload(version: CloudServerVersion): Boolean

    fun isDownloaded(version: CloudServerVersion): Boolean = getJar(version).exists()

    suspend fun isUpdateAvailable(version: CloudServerVersion, force: Boolean = false): Boolean

    suspend fun getVersions(version: CloudServerVersion): List<ServerVersion>

    suspend fun getBuilds(version: CloudServerVersion, mcVersion: ServerVersion): List<String>

    suspend fun update(version: CloudServerVersion): File

    suspend fun patch(version: CloudServerVersion)

    fun isPatched(version: CloudServerVersion): Boolean =
        version.libPattern != null && File(getFolder(version).absolutePath, ".patched").exists()

    fun isPatchVersion(version: CloudServerVersion): Boolean = version.libPattern != null

    fun getJar(version: CloudServerVersion): File = File(getFolder(version).absolutePath, "$version.name.jar")

    fun getFolder(version: CloudServerVersion): File =
        File(MINECRAFT_VERSIONS_FOLDER.getFile().absolutePath, version.name)

    fun register() = registerHandler(this)

    companion object {

        private val CACHE = mutableListOf<IServerVersionHandler>()

        fun getHandler(type: CloudServerVersionType): IServerVersionHandler =
            CACHE.firstOrNull { it.name.lowercase() == type.versionHandlerName.lowercase() }
                ?: throw IllegalStateException("No handler for type $type")

        fun registerHandler(serverVersionHandler: IServerVersionHandler): IServerVersionHandler {
            if (CACHE.any { it.name.lowercase() == serverVersionHandler.name.lowercase() })
                return CACHE.first { it.name.lowercase() == serverVersionHandler.name.lowercase() }
            CACHE.add(serverVersionHandler)
            return serverVersionHandler
        }

        suspend fun registerDefaultHandlers(serverVersionRepository: ServerVersionRepository) {
            registerHandler(URLServerVersionHandler(serverVersionRepository))
            registerHandler(PaperMcServerVersionHandler(serverVersionRepository))
        }

    }

}