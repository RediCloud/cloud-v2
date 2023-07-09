package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.requester.PaperMcApiRequester
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.*
import khttp.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration.Companion.minutes

class PaperMcServerVersionHandler(
    override val serverVersionRepository: CloudServerVersionRepository,
    val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    val javaVersionRepository: JavaVersionRepository,
    override val nodeRepository: NodeRepository,
    override val console: Console,
    override val name: String = "papermc",
    override var lastUpdateCheck: Long = -1
) : URLServerVersionHandler(serverVersionRepository, nodeRepository, console, serverVersionTypeRepository, javaVersionRepository) {

    companion object {
        private val logger = LogManager.logger(PaperMcServerVersionHandler::class)
    }

    private val requester = PaperMcApiRequester()
    private val locks = mutableMapOf<UUID, ReentrantLock>()

    override fun getLock(version: CloudServerVersion): ReentrantLock {
        return locks.getOrPut(version.uniqueId) { ReentrantLock() }
    }

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        var canceled = false
        var downloaded = false
        var error = false
        val animation = AnimatedLineAnimation(
            console,
            200
        ) {
            if (canceled) {
                null
            } else if (downloaded) {
                canceled = true
                "Downloading version %hc%${version.getDisplayName()}§8: ${if (error) "§4✘" else "§2✓"}"
            } else {
                "Downloading version %hc%${version.getDisplayName()}§8: %tc%%loading%"
            }
        }
        console.startAnimation(animation)
        getLock(version).lock()
        val jar = getJar(version)
        try {
            if (jar.exists() && !force) return jar
            if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")

            val type = serverVersionTypeRepository.getType(version.typeId!!)
                ?: throw NullPointerException("Cant find server version type ${version.typeId}")

            val buildId = requester.getLatestBuild(type, version.version)
            if (buildId == -1) throw NullPointerException("Cant find build for ${version.getDisplayName()}")

            val url = requester.getDownloadUrl(type, version.version, buildId)
            val response = get(url)
            if (response.statusCode != 200) throw IllegalStateException("Download of ${version.version} is not available (${response.statusCode}):\n${response.text}")

            val folder = getFolder(version)
            if (folder.exists()) folder.deleteRecursively()
            folder.mkdirs()
            if (jar.exists()) jar.delete()
            jar.writeBytes(response.content)

            version.buildId = buildId.toString()
            serverVersionRepository.updateVersion(version)

            val downloader = MultiAsyncAction()

            val defaultFiles = mutableMapOf<String, String>()
            defaultFiles.putAll(version.defaultFiles)
            defaultFiles.putAll(type.defaultFiles)
            defaultFiles.forEach {
                downloader.add {
                    val url1 = it.value
                    val path = it.key
                    try {
                        if (!isValidUrl(url1)) {
                            logger.warning(
                                "§cInvalid default file with url ${
                                    toConsoleValue(
                                        url1,
                                        false
                                    )
                                } for ${toConsoleValue(version.getDisplayName(), false)}"
                            )
                            return@add
                        }
                        val file = File(folder, path)
                        if (!file.parentFile.exists()) file.parentFile.mkdirs()
                        val response1 = get(url1)
                        if (response1.statusCode != 200) {
                            logger.warning(
                                "§cDownload of default file ${
                                    toConsoleValue(
                                        url1,
                                        false
                                    )
                                } for ${
                                    toConsoleValue(
                                        version.getDisplayName(),
                                        false
                                    )
                                } is not available (${response.statusCode}):\n${response.text}"
                            )
                            return@add
                        }
                        file.writeBytes(response1.content)
                    }catch (e: Exception) {
                        logger.warning("§cFailed to download default file ${toConsoleValue(url1, false)} for ${toConsoleValue(version.getDisplayName(), false)}", e)
                    }
                }
            }

            downloader.joinAll()

            lastUpdateCheck = System.currentTimeMillis()
        }catch (e: Exception) {
            error = true
            throw e
        } finally {
            downloaded = true
            getLock(version).unlock()
        }

        return jar
    }

    override suspend fun canDownload(version: CloudServerVersion): Boolean {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val buildId = requester.getLatestBuild(type, version.version)
        if (buildId == -1) return false
        val url = requester.getDownloadUrl(type, version.version, buildId)
        return isValidUrl(url)
    }

    override suspend fun isUpdateAvailable(version: CloudServerVersion, force: Boolean): Boolean {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        if (!force && System.currentTimeMillis() - lastUpdateCheck < 5.minutes.inWholeMilliseconds) return false
        val currentId = version.buildId ?: return true
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val latest = requester.getLatestBuild(type, version.version)
        if (latest == -1) throw NullPointerException("Cant find build for ${version.getDisplayName()}")
        lastUpdateCheck = System.currentTimeMillis()
        return latest > currentId.toInt()
    }

    override suspend fun getVersions(version: CloudServerVersion): List<ServerVersion> {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getVersions(type)
    }

    override suspend fun getBuilds(version: CloudServerVersion, mcVersion: ServerVersion): List<String> {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getBuilds(type, mcVersion).map { it.toString() }
    }


    override suspend fun update(version: CloudServerVersion): File {
        download(version, true)
        if (isPatchVersion(version)) patch(version)
        return getFolder(version)
    }

}