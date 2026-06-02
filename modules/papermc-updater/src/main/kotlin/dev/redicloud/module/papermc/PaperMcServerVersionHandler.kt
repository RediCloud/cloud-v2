package dev.redicloud.module.papermc

import dev.redicloud.api.exceptions.CloudVersionException
import dev.redicloud.api.java.ICloudJavaVersion
import dev.redicloud.api.java.ICloudJavaVersionRepository
import dev.redicloud.api.utils.ProcessConfiguration
import dev.redicloud.api.utils.TEMP_SERVER_VERSION_FOLDER
import dev.redicloud.api.version.*
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.utils.ScreenProcessHandler
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.Logger
import dev.redicloud.utils.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.statement.readRawBytes
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

@Suppress("TooManyFunctions")
class PaperMcServerVersionHandler(
    private val serverVersionRepository: ICloudServerVersionRepository,
    private val serverVersionTypeRepository: ICloudServerVersionTypeRepository,
    private val javaVersionRepository: ICloudJavaVersionRepository,
    private val requester: PaperMcApiRequester,
    private val console: Console?,
    private val logger: Logger
) : IServerVersionHandler {

    companion object {
        private const val ANIMATION_TICK_MS = 200L
        private const val PATCH_PORT_RANGE_START = 40000
        private const val PATCH_PORT_RANGE_END = 60000
    }

    override val name: String = "papermc"
    override val default: Boolean = false
    private val lastUpdateChecks = mutableMapOf<ICloudServerVersion, Long>()

    override suspend fun download(version: ICloudServerVersion, force: Boolean, lock: Boolean): File {
        var canceled = false
        var downloaded = false
        var error = false
        console?.let {
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
        }
        return getLock(version).withOptionalLock(lock) {
            val jar = getJar(version)
            @Suppress("TooGenericExceptionCaught")
            try {
                if (jar.exists() && !force) return@withOptionalLock jar
                downloadJar(version, jar)
                downloadDefaultFiles(version, getFolder(version), logger)
                lastUpdateChecks[version] = System.currentTimeMillis()
            } catch (e: CloudVersionException) {
                error = true
                throw e
            } catch (e: Exception) {
                error = true
                throw CloudVersionException("Failed to download version ${version.displayName}", e)
            } finally {
                downloaded = true
            }
            jar
        }
    }

    @Suppress("ThrowsCount")
    private suspend fun downloadJar(version: ICloudServerVersion, jar: File) {
        if (version.typeId == null) {
            throw NullPointerException("Cant find server version type for ${version.displayName}")
        }
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")

        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val buildId = requester.getLatestBuild(type, targetVersion)
        if (buildId == -1) throw NullPointerException("Cant find build for ${version.displayName}")

        val url = requester.getDownloadUrl(type, targetVersion, buildId)
        val response = httpClient.get { url(url) }
        check(response.status.isSuccess()) {
            "Download of ${targetVersion.name} is not available (${response.status.value}):\n${response.bodyAsText()}"
        }

        val folder = getFolder(version)
        val bytes = response.readRawBytes()
        withContext(Dispatchers.IO) {
            if (folder.exists()) folder.deleteRecursively()
            folder.mkdirs()
            if (jar.exists()) jar.delete()
            jar.writeBytes(bytes)
        }

        version.buildId = buildId.toString()
        serverVersionRepository.updateVersion(version)
    }

    private suspend fun downloadDefaultFiles(version: ICloudServerVersion, folder: File, logger: Logger) {
        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: return
        val downloader = ConcurrentBatch()
        val defaultFiles = mutableMapOf<String, String>()
        defaultFiles.putAll(version.defaultFiles)
        defaultFiles.putAll(type.defaultFiles)
        defaultFiles.forEach {
            downloader.add {
                val url = it.value
                    .replace("%build%", BUILD)
                    .replace("%cloud_version%", CLOUD_VERSION)
                    .replace("%branch%", BRANCH)
                val path = it.key
                downloadSingleDefaultFile(url, path, folder, version, logger)
            }
        }
        downloader.joinAll()
    }

    private suspend fun downloadSingleDefaultFile(
        url: String,
        path: String,
        folder: File,
        version: ICloudServerVersion,
        logger: Logger
    ) {
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
                    "§cDownload of default file ${toConsoleValue(url, false)} for " +
                        "${toConsoleValue(version.displayName, false)} is not available " +
                        "(${response.status.value}):\n${response.bodyAsText()}"
                )
                return
            }
            val bytes = response.readRawBytes()
            withContext(Dispatchers.IO) {
                file.createNewFile()
                file.writeBytes(bytes)
            }
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
        if (version.typeId == null) {
            throw NullPointerException(
                "Cant find server version type for ${version.displayName}"
            )
        }
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val buildId = requester.getLatestBuild(type, targetVersion)
        if (buildId == -1) return false
        val url = requester.getDownloadUrl(type, targetVersion, buildId)
        return isValidUrl(url)
    }

    override suspend fun isUpdateAvailable(version: ICloudServerVersion, force: Boolean): Boolean {
        if (version.typeId == null) {
            throw NullPointerException(
                "Cant find server version type for ${version.displayName}"
            )
        }
        if (!force && System.currentTimeMillis() - (lastUpdateChecks[version] ?: -1) < 5.minutes.inWholeMilliseconds) return false
        val currentId = version.buildId ?: return true
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val targetVersion = if (version.version.latest) version.version.dynamicVersion() else version.version
        val latest = requester.getLatestBuild(type, targetVersion)
        if (latest == -1) throw NullPointerException("Cant find build for ${version.displayName}")
        lastUpdateChecks[version] = System.currentTimeMillis()
        return latest > currentId.toInt()
    }

    override suspend fun getVersions(version: ICloudServerVersion): List<IServerVersion> {
        if (version.typeId == null) {
            throw NullPointerException(
                "Cant find server version type for ${version.displayName}"
            )
        }
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getVersions(type)
    }

    override suspend fun getBuilds(version: ICloudServerVersion, mcVersion: IServerVersion): List<String> {
        if (version.typeId == null) {
            throw NullPointerException(
                "Cant find server version type for ${version.displayName}"
            )
        }
        val type = serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getBuilds(type, mcVersion).map { it.toString() }
    }

    override suspend fun update(version: ICloudServerVersion, versionType: ICloudServerVersionType): File {
        download(version, true)
        if (isPatchVersion(version)) patch(version)
        serverVersionTypeRepository.downloadConnector(versionType)
        return getFolder(version)
    }

    override suspend fun patch(version: ICloudServerVersion, lock: Boolean) {
        if (!version.patch) return
        var canceled = false
        var patched = false
        var error = false
        console?.let {
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
        }
        getLock(version).withOptionalLock(lock) {
            @Suppress("TooGenericExceptionCaught")
            try {
                val jar = getJar(version)
                if (!jar.exists()) download(version, true, lock = false)

                val versionDir = getFolder(version)
                val tempDir = File(TEMP_SERVER_VERSION_FOLDER.getFile().absolutePath, UUID.randomUUID().toString())
                withContext(Dispatchers.IO) {
                    tempDir.mkdirs()
                    versionDir.copyRecursively(tempDir, true)
                }
                val tempJar = File(tempDir, jar.name)

                val type = resolveVersionType(version)
                val javaVersion = resolveJavaVersion(version)
                executePatchProcess(version, type, javaVersion, tempDir, tempJar)

                withContext(Dispatchers.IO) {
                    if (!versionDir.exists()) versionDir.mkdirs()
                    tempJar.copyTo(jar, true)
                    cleanupPatchedFiles(version, type, tempDir, tempJar)
                    versionDir.deleteRecursively()
                    tempDir.copyRecursively(versionDir, true)
                    tempDir.deleteRecursively()
                    File(versionDir, ".patched").createNewFile()
                }
            } catch (e: CloudVersionException) {
                error = true
                throw e
            } catch (e: Exception) {
                error = true
                throw CloudVersionException("Failed to patch version ${version.displayName}", e)
            } finally {
                patched = true
            }
        }
    }

    private suspend fun resolveVersionType(version: ICloudServerVersion): ICloudServerVersionType {
        if (version.typeId == null) {
            throw NullPointerException("Cant find server version type for ${version.displayName}")
        }
        return serverVersionTypeRepository.getType(version.typeId!!)
            ?: throw NullPointerException("Cant find server version type ${version.typeId}")
    }

    private suspend fun resolveJavaVersion(version: ICloudServerVersion): ICloudJavaVersion {
        if (version.javaVersionId == null) {
            throw NullPointerException("Cant find java version for ${version.displayName}")
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
        withContext(Dispatchers.IO) {
            val process = processBuilder.start()
            console?.let {
                val screen = console.createScreen("patch_${version.displayName}")
                ScreenProcessHandler(process, screen)
            }
            process.waitFor(5.minutes.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        }
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

    override suspend fun patchCommand(
        type: ICloudServerVersionType,
        javaVersion: ICloudJavaVersion,
        jarToExecute: File
    ): List<String> {
        return IServerVersionHandler.getDefaultHandler().patchCommand(type, javaVersion, jarToExecute)
    }

    override fun getLock(version: ICloudServerVersion): Mutex {
        return IServerVersionHandler.getDefaultHandler().getLock(version)
    }
}
