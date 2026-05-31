package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.api.exceptions.CloudVersionException
import dev.redicloud.api.java.ICloudJavaVersion
import dev.redicloud.api.java.ICloudJavaVersionRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.utils.ProcessConfiguration
import dev.redicloud.api.utils.TEMP_SERVER_VERSION_FOLDER
import dev.redicloud.api.version.*
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.utils.ScreenProcessHandler
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

@Suppress("TooManyFunctions")
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
        private const val ANIMATION_TICK_MS = 200L
        private const val PATCH_PORT_RANGE_START = 40000
        private const val PATCH_PORT_RANGE_END = 60000
    }

    protected val locks = mutableMapOf<UUID, SimpleLock>()

    override fun getLock(version: ICloudServerVersion): SimpleLock {
        return locks.getOrPut(version.uniqueId) { SimpleLock() }
    }

    override suspend fun download(version: ICloudServerVersion, force: Boolean, lock: Boolean): File {
        var canceled = false
        var downloaded = false
        var error = false
        val animation = AnimatedLineAnimation(
            console,
            ANIMATION_TICK_MS
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
        if (lock) getLock(version).lock()
        val jar = getJar(version)
        @Suppress("TooGenericExceptionCaught")
        try {
            if (jar.exists() && !force) return jar
            downloadJar(version, jar)
            downloadDefaultFiles(version, getFolder(version))
        } catch (e: CloudVersionException) {
            error = true
            throw e
        } catch (e: Exception) {
            error = true
            throw CloudVersionException("Failed to download version ${version.displayName}", e)
        } finally {
            downloaded = true
            if (lock) getLock(version).unlock()
        }
        return jar
    }

    @Suppress("ThrowsCount")
    private suspend fun downloadJar(version: ICloudServerVersion, jar: File) {
        if (version.typeId == null) {
            throw NullPointerException(
                "Cant find server version type for ${version.displayName}"
            )
        }
        serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        if (version.customDownloadUrl == null) {
            throw NullPointerException(
                "Download url of ${version.displayName} is null"
            )
        }

        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val downloadUrl = version.customDownloadUrl!!
            .replace("%build%", version.buildId ?: "-1")
            .replace("%version_name%", targetVersion.name)
            .replace("%branch%", BRANCH)

        val response = httpClient.get { url(downloadUrl) }
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                "Download of ${version.displayName} is not available ($downloadUrl -> ${response.status.value}):\n" +
                    response.bodyAsText()
            )
        }

        val folder = getFolder(version)
        if (folder.exists()) folder.deleteRecursively()
        folder.mkdirs()
        if (jar.exists()) jar.delete()
        jar.writeBytes(response.readBytes())
    }

    private suspend fun downloadDefaultFiles(version: ICloudServerVersion, folder: File) {
        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: return
        val downloader = MultiAsyncAction()
        val defaultFiles = mutableMapOf<String, String>()
        defaultFiles.putAll(version.defaultFiles)
        defaultFiles.putAll(type.defaultFiles)
        defaultFiles.forEach {
            downloader.add {
                val url = it.value
                    .replace("%build%", BUILD)
                    .replace("%cloud_version%", CLOUD_VERSION)
                    .replace("%branch%", BRANCH)
                downloadSingleDefaultFile(url, it.key, folder, version)
            }
        }
        downloader.joinAll()
    }

    private suspend fun downloadSingleDefaultFile(url: String, path: String, folder: File, version: ICloudServerVersion) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (!isValidUrl(url)) {
                logger.warning(
                    "§cInvalid default file with url ${toConsoleValue(
                        url,
                        false
                    )} for ${toConsoleValue(version.displayName, false)}"
                )
                return
            }
            val file = File(folder, path)
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            val response = httpClient.get { url(url) }
            if (!response.status.isSuccess()) {
                logger.warning(
                    "§cDownload of default file ${toConsoleValue(url, false)} " +
                        "for ${toConsoleValue(version.displayName, false)} is not available (${response.status.value}):\n${response.bodyAsText()}"
                )
                return
            }
            file.createNewFile()
            file.writeBytes(response.readBytes())
        } catch (e: Exception) {
            logger.warning(
                "§cFailed to download default file ${toConsoleValue(
                    url,
                    false
                )} for ${toConsoleValue(version.displayName, false)}",
                e
            )
        }
    }

    override suspend fun canDownload(version: ICloudServerVersion): Boolean {
        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val downloadUrl = version.customDownloadUrl!!
            .replace("%build%", version.buildId ?: "-1")
            .replace("%version_name%", targetVersion.name)
            .replace("%branch%", BRANCH)
        return version.customDownloadUrl != null && isValidUrl(downloadUrl)
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
        if (!javaVersion.isLocated(serviceId)) {
            javaVersion.located[serviceId.id] = javaVersion.autoLocate()?.absolutePath ?: error("Java version ${javaVersion.id} not found")
        }
        val javaPath = javaVersion.located[serviceId.id]
        check(!javaPath.isNullOrEmpty()) { "Java version ${javaVersion.id} not found" }

        val list = mutableListOf(
            javaPath,
            "-Xms512M",
            "-Xmx512M",
        )
        list.add("-jar")
        list.add(jarToExecute.absolutePath)
        return list
    }

    override suspend fun patch(version: ICloudServerVersion, lock: Boolean) {
        if (!version.patch) return
        var canceled = false
        var patched = false
        var error = false
        val animation = AnimatedLineAnimation(
            console,
            ANIMATION_TICK_MS
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
        if (lock) getLock(version).lock()
        @Suppress("TooGenericExceptionCaught")
        try {
            val jar = getJar(version)
            if (!jar.exists()) download(version, true, lock = false)

            val versionDir = getFolder(version)
            val tempDir = File(TEMP_SERVER_VERSION_FOLDER.getFile().absolutePath, UUID.randomUUID().toString())
            tempDir.mkdirs()
            versionDir.copyRecursively(tempDir, true)
            val tempJar = File(tempDir, jar.name)

            val type = resolveVersionType(version)
            val javaVersion = resolveJavaVersion(version)
            executePatchProcess(version, type, javaVersion, tempDir, tempJar)

            if (!versionDir.exists()) versionDir.mkdirs()
            tempJar.copyTo(jar, true)
            cleanupPatchedFiles(version, type, tempDir, tempJar)
            versionDir.deleteRecursively()
            tempDir.copyRecursively(versionDir, true)
            tempDir.deleteRecursively()
            File(versionDir, ".patched").createNewFile()
        } catch (e: CloudVersionException) {
            error = true
            throw e
        } catch (e: Exception) {
            error = true
            throw CloudVersionException("Failed to patch version ${version.displayName}", e)
        } finally {
            patched = true
            if (lock) getLock(version).unlock()
        }
    }

    private suspend fun resolveVersionType(version: ICloudServerVersion): ICloudServerVersionType {
        if (version.typeId == null) {
            throw NullPointerException(
                "Cant find server version type for ${version.displayName}"
            )
        }
        return serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
    }

    private suspend fun resolveJavaVersion(version: ICloudServerVersion): ICloudJavaVersion {
        if (version.javaVersionId == null) {
            throw NullPointerException(
                "Cant find java version for ${version.displayName}"
            )
        }
        return javaVersionRepository.getVersion(version.javaVersionId!!)
            ?: throw NullPointerException("Cant find java version for ${version.displayName}")
    }

    private suspend fun executePatchProcess(
        version: ICloudServerVersion,
        type: ICloudServerVersionType,
        javaVersion: ICloudJavaVersion,
        tempDir: File,
        tempJar: File
    ) {
        findFreePort(PATCH_PORT_RANGE_START..PATCH_PORT_RANGE_END)
        val processBuilder = ProcessBuilder(patchCommand(type, javaVersion, tempJar))
        processBuilder.directory(tempDir)
        val process = processBuilder.start()
        val screen = console.createScreen("patch_${version.displayName}")
        ScreenProcessHandler(process, screen)
        process.waitFor(5.minutes.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    private fun cleanupPatchedFiles(
        version: ICloudServerVersion,
        type: ICloudServerVersionType,
        tempDir: File,
        tempJar: File
    ) {
        val processConfiguration = ProcessConfiguration.collect(version, type)
        val patterns = processConfiguration.getLibPatterns().toMutableList()
        if (version.libPattern != null) patterns.add(Pattern.compile(version.libPattern!!))
        if (type.libPattern != null) patterns.add(Pattern.compile(type.libPattern!!))
        if (patterns.isEmpty()) return

        patterns.add(Pattern.compile("(${tempJar.name})"))
        val defaultFilePaths = processConfiguration.defaultFiles.values

        tempDir.listFiles()?.forEach { deleteNonLibFiles(it, tempDir, defaultFilePaths, patterns) }
    }

    private fun deleteNonLibFiles(
        file: File,
        baseDir: File,
        defaultFilePaths: Collection<String>,
        patterns: List<Pattern>
    ): Boolean {
        var workDirPath = file.absolutePath.replace(baseDir.absolutePath, "").replace("\\", "/")
        if (workDirPath.startsWith("/")) workDirPath = workDirPath.substring(1)
        if (defaultFilePaths.any { file.absolutePath.endsWith(it) }) return false
        if (patterns.none { it.matcher(workDirPath).find() }) {
            if (file.isDirectory) {
                if (file.listFiles()?.all { deleteNonLibFiles(it, baseDir, defaultFilePaths, patterns) } == true) {
                    file.deleteRecursively()
                }
            } else {
                file.delete()
                return true
            }
        }
        return false
    }
}
