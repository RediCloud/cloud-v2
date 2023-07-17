package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.api.repositories.java.ICloudJavaVersionRepository
import dev.redicloud.api.repositories.version.ICloudServerVersion
import dev.redicloud.api.repositories.version.ICloudServerVersionRepository
import dev.redicloud.api.repositories.version.ICloudServerVersionTypeRepository
import dev.redicloud.api.repositories.version.IServerVersion
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.version.requester.PaperMcApiRequester
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import khttp.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration.Companion.minutes

class PaperMcServerVersionHandler(
    serviceId: ServiceId,
    serverVersionRepository: ICloudServerVersionRepository,
    serverVersionTypeRepository: ICloudServerVersionTypeRepository,
    javaVersionRepository: ICloudJavaVersionRepository,
    nodeRepository: NodeRepository,
    console: Console,
) : URLServerVersionHandler(
    serviceId,
    serverVersionRepository,
    serverVersionTypeRepository,
    nodeRepository,
    console,
    javaVersionRepository,
    false,
    "papermc"
) {


    companion object {
        private val logger = LogManager.logger(PaperMcServerVersionHandler::class)
    }

    private val requester = PaperMcApiRequester()

    override suspend fun download(version: ICloudServerVersion, force: Boolean): File {
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
                "Downloading version %hc%${version.displayName}§8: ${if (error) "§4✘" else "§2✓"}"
            } else {
                "Downloading version %hc%${version.displayName}§8: %tc%%loading%"
            }
        }
        console.startAnimation(animation)
        getLock(version).lock()
        val jar = getJar(version)
        try {
            if (jar.exists() && !force) return jar
            if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.displayName}")

            val type = serverVersionTypeRepository.getType(version.typeId!!)
                ?: throw NullPointerException("Cant find server version type ${version.typeId}")

            val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
            val buildId = requester.getLatestBuild(type, targetVersion)
            if (buildId == -1) throw NullPointerException("Cant find build for ${version.displayName}")

            val url = requester.getDownloadUrl(type, targetVersion, buildId)
            val response = get(url)
            if (response.statusCode != 200) throw IllegalStateException("Download of ${targetVersion.name} is not available (${response.statusCode}):\n${response.text}")

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
                    val url1 =
                        it.value.replace("%build_number%", BUILD_NUMBER).replace("%cloud_version%", CLOUD_VERSION)
                    val path = it.key
                    try {
                        if (!isValidUrl(url1)) {
                            logger.warning(
                                "§cInvalid default file with url ${
                                    toConsoleValue(
                                        url1,
                                        false
                                    )
                                } for ${toConsoleValue(version.displayName, false)}"
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
                                        version.displayName,
                                        false
                                    )
                                } is not available (${response.statusCode}):\n${response.text}"
                            )
                            return@add
                        }
                        file.createNewFile()
                        file.writeBytes(response1.content)
                    } catch (e: Exception) {
                        logger.warning(
                            "§cFailed to download default file ${
                                toConsoleValue(
                                    url1,
                                    false
                                )
                            } for ${toConsoleValue(version.displayName, false)}", e
                        )
                    }
                }
            }

            downloader.joinAll()

            lastUpdateCheck = System.currentTimeMillis()
        } catch (e: Exception) {
            error = true
            throw e
        } finally {
            downloaded = true
            getLock(version).unlock()
        }

        return jar
    }

    override suspend fun canDownload(version: ICloudServerVersion): Boolean {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.displayName}")
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val buildId = requester.getLatestBuild(type, targetVersion)
        if (buildId == -1) return false
        val url = requester.getDownloadUrl(type, targetVersion, buildId)
        return isValidUrl(url)
    }

    override suspend fun isUpdateAvailable(version: ICloudServerVersion, force: Boolean): Boolean {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.displayName}")
        if (!force && System.currentTimeMillis() - lastUpdateCheck < 5.minutes.inWholeMilliseconds) return false
        val currentId = version.buildId ?: return true
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val latest = requester.getLatestBuild(type, targetVersion)
        if (latest == -1) throw NullPointerException("Cant find build for ${version.displayName}")
        lastUpdateCheck = System.currentTimeMillis()
        return latest > currentId.toInt()
    }

    override suspend fun getVersions(version: ICloudServerVersion): List<IServerVersion> {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.displayName}")
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getVersions(type)
    }

    override suspend fun getBuilds(version: ICloudServerVersion, mcVersion: IServerVersion): List<String> {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.displayName}")
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getBuilds(type, mcVersion).map { it.toString() }
    }
}