package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.api.java.ICloudJavaVersion
import dev.redicloud.api.java.ICloudJavaVersionRepository
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.version.*
import dev.redicloud.api.utils.ProcessConfiguration
import dev.redicloud.api.utils.TEMP_SERVER_VERSION_FOLDER
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.console.utils.ScreenProcessHandler
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.*
import dev.redicloud.api.service.ServiceId
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

open class URLServerVersionHandler(
    protected val serviceId: ServiceId,
    protected val serverVersionRepository: ICloudServerVersionRepository,
    protected val serverVersionTypeRepository: ICloudServerVersionTypeRepository,
    protected val nodeRepository: ICloudNodeRepository,
    protected val console: Console,
    protected val javaVersionRepository: ICloudJavaVersionRepository,
    override val default: Boolean = true,
    override val name: String = "urldownloader"
) : IServerVersionHandler {

    companion object {
        private val logger = LogManager.logger(URLServerVersionHandler::class)
    }

    protected var lastUpdateCheck: Long = -1
    protected val locks = mutableMapOf<UUID, ReentrantLock>()

    override fun getLock(version: ICloudServerVersion): ReentrantLock {
        return locks.getOrPut(version.uniqueId) { ReentrantLock() }
    }

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
                "Downloaded version %hc%${version.displayName}§8: ${if (error) "§4✘" else "§2✓"}"
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
            if (version.customDownloadUrl == null) throw NullPointerException("Download url of ${version.displayName} is null")

            val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
            val downloadUrl = version.customDownloadUrl!!
                .replace("%build%", version.buildId ?: "-1")
                .replace("%version_name%", targetVersion.name)
                .replace("%branch%", BRANCH)

            val response = get(downloadUrl)
            if (response.statusCode != 200) throw IllegalStateException(
                "Download of ${version.displayName} is not available ($downloadUrl -> ${response.statusCode}):\n" +
                        response.text
            )

            val folder = getFolder(version)
            if (folder.exists()) folder.deleteRecursively()
            folder.mkdirs()
            if (jar.exists()) jar.delete()
            jar.writeBytes(response.content)

            val downloader = MultiAsyncAction()
            val defaultFiles = mutableMapOf<String, String>()
            defaultFiles.putAll(version.defaultFiles)
            defaultFiles.putAll(type.defaultFiles)
            defaultFiles.forEach {
                downloader.add {
                    val url1 = it.value
                        .replace("%build%", BUILD)
                        .replace("%cloud_version%", CLOUD_VERSION)
                        .replace("%branch%", BRANCH)
                    val path = it.key
                    try {
                        if (!isValidUrl(url1)) {
                            logger.warning("§cInvalid default file with url ${toConsoleValue(url1, false)} for ${toConsoleValue(version.displayName, false)}")
                            return@add
                        }
                        val file = File(folder, path)
                        if (!file.parentFile.exists()) file.parentFile.mkdirs()
                        val response1 = get(url1)
                        if (response1.statusCode != 200) {
                            logger.warning("§cDownload of default file ${toConsoleValue(url1, false)} for ${toConsoleValue(version.displayName, false)} is not available (${response.statusCode}):\n${response.text}")
                            return@add
                        }
                        file.createNewFile()
                        file.writeBytes(response1.content)
                    }catch (e: Exception) {
                        logger.warning("§cFailed to download default file ${toConsoleValue(url1, false)} for ${toConsoleValue(version.displayName, false)}", e)
                    }
                }
            }

            downloader.joinAll()
        }catch (e: Exception) {
            error = true
            throw e
        }finally {
            downloaded = true
            getLock(version).unlock()
        }
        return jar
    }

    override suspend fun canDownload(version: ICloudServerVersion): Boolean {
        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val downloadUrl = version.customDownloadUrl!!
            .replace("%build%", version.buildId ?: "-1")
            .replace("%version_name%", targetVersion.name)
            .replace("%branch%", BRANCH)
        return version.customDownloadUrl != null && get(downloadUrl).statusCode == 200
    }

    override suspend fun isUpdateAvailable(version: ICloudServerVersion, force: Boolean): Boolean {
        return false
    }

    override suspend fun getVersions(version: ICloudServerVersion): List<IServerVersion> = emptyList()

    override suspend fun getBuilds(version: ICloudServerVersion, mcVersion: IServerVersion): List<String> = emptyList()

    override suspend fun update(version: ICloudServerVersion, versionType: ICloudServerVersionType): File {
        download(version, true)
        if (isPatchVersion(version)) patch(version)
        serverVersionTypeRepository.downloadConnector(versionType)
        return getFolder(version)
    }

    override suspend fun patchCommand(
        type: ICloudServerVersionType,
        javaVersion: ICloudJavaVersion,
        jarToExecute: File
    ): List<String> {
        if(!javaVersion.isLocated(serviceId)) {
            javaVersion.located[serviceId.id] = javaVersion.autoLocate()?.absolutePath ?: throw IllegalStateException("Java version ${javaVersion.id} not found")
        }
        val javaPath = javaVersion.located[serviceId.id]
        if (javaPath == null || javaPath.isEmpty()) throw IllegalStateException("Java version ${javaVersion.id} not found")

        val list = mutableListOf(
            javaPath,
            "-Xms512M",
            "-Xmx512M",
        )
        list.add("-jar")
        list.add(jarToExecute.absolutePath)
        return list
    }

    override suspend fun patch(version: ICloudServerVersion) {
        if (!version.patch) return
        var canceled = false
        var patched = false
        var error = false
        val animation = AnimatedLineAnimation(
            console,
            200
        ) {
            if (canceled) {
                null
            } else if (patched) {
                canceled = true
                "Patching version %tc%${toConsoleValue(version.displayName)}§8: ${if (error) "§4✘" else "§2✓"}"
            } else {
                "Patching version %tc%${toConsoleValue(version.displayName)}§8: %tc%%loading%"
            }
        }
        console.startAnimation(animation)
        getLock(version).lock()
        try {
            val jar = getJar(version)
            if (!jar.exists()) download(version, true)

            val versionDir = getFolder(version)
            val tempDir = File(TEMP_SERVER_VERSION_FOLDER.getFile().absolutePath, UUID.randomUUID().toString())
            tempDir.mkdirs()
            versionDir.copyRecursively(tempDir, true)
            val tempJar = File(tempDir, jar.name)

            if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.displayName}")
            val type = serverVersionTypeRepository.getType(version.typeId!!)
                ?: throw NullPointerException("Cant find server version type ${version.typeId}")
            if (version.javaVersionId == null) throw NullPointerException("Cant find java version for ${version.displayName}")
            val javaVersion = javaVersionRepository.getVersion(version.javaVersionId!!)
                ?: throw NullPointerException("Cant find java version for ${version.displayName}")
            findFreePort(40000..60000)

            val processBuilder = ProcessBuilder(patchCommand(type, javaVersion, tempJar))
            processBuilder.directory(tempDir)
            val process = processBuilder.start()
            val screen = console.createScreen("patch_${version.displayName}")
            ScreenProcessHandler(process, screen)
            process.waitFor(5.minutes.inWholeMilliseconds, TimeUnit.MILLISECONDS)

            if (!versionDir.exists()) versionDir.mkdirs()

            tempJar.copyTo(jar, true)
            val processConfiguration = ProcessConfiguration.collect(
                version,
                type
            )
            val patterns = processConfiguration.getLibPatterns().toMutableList()
            if (version.libPattern != null) patterns.add(Pattern.compile(version.libPattern!!))
            if (type.libPattern != null) patterns.add(Pattern.compile(type.libPattern!!))
            if (patterns.isNotEmpty()) {
                patterns.add(Pattern.compile("(${tempJar.name})"))

                fun deleteFiles(file: File): Boolean {
                    val paths = processConfiguration.defaultFiles.values
                    var workDirPath = file.absolutePath.replace(tempDir.absolutePath, "").replace("\\", "/")
                    if (workDirPath.startsWith("/")) workDirPath = workDirPath.substring(1)
                    if (paths.any { file.absolutePath.endsWith(it) }) return false
                    if (patterns.none { it.matcher(workDirPath).find() }) {
                        if (file.isDirectory) {
                            if (file.listFiles()?.all { deleteFiles(it) } == true) file.deleteRecursively()
                        } else {
                            file.delete()
                            return true
                        }
                    }
                    return false
                }

                tempDir.listFiles()?.forEach {
                    deleteFiles(it)
                }
            }
            versionDir.deleteRecursively()
            tempDir.copyRecursively(versionDir, true)
            tempDir.deleteRecursively()
            File(versionDir, ".patched").createNewFile()
        }catch (e: Exception) {
            error = true
            throw e
        } finally {
            patched = true
            getLock(version).unlock()
        }
    }
}