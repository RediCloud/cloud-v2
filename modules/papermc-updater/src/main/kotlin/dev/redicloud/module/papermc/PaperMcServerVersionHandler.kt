package dev.redicloud.module.papermc

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
import io.ktor.http.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class PaperMcServerVersionHandler(
    private val serverVersionRepository: ICloudServerVersionRepository,
    private val serverVersionTypeRepository: ICloudServerVersionTypeRepository,
    private val javaVersionRepository: ICloudJavaVersionRepository,
    private val requester: PaperMcApiRequester,
    private val console: Console?,
    private val logger: Logger
) : IServerVersionHandler {

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
        }
        if (lock) getLock(version).lock()
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
            val response = httpClient.get { url(url) }
            if (!response.status.isSuccess()) throw IllegalStateException("Download of ${targetVersion.name} is not available (${response.status.value}):\n${response.bodyAsText()}")

            val folder = getFolder(version)
            if (folder.exists()) folder.deleteRecursively()
            folder.mkdirs()
            if (jar.exists()) jar.delete()
            jar.writeBytes(response.readBytes())

            version.buildId = buildId.toString()
            serverVersionRepository.updateVersion(version)

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
                        val response1 = httpClient.get { url(url1) }
                        if (!response1.status.isSuccess()) {
                            logger.warning(
                                "§cDownload of default file " +
                                        "${toConsoleValue(url1, false)} for " +
                                        "${toConsoleValue(version.displayName, false)} is not available " +
                                        "(${response.status.value}):\n${response.bodyAsText()}"
                            )
                            return@add
                        }
                        file.createNewFile()
                        file.writeBytes(response1.readBytes())
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

            lastUpdateChecks[version] = System.currentTimeMillis()
        } catch (e: Exception) {
            error = true
            throw e
        } finally {
            downloaded = true
            if (lock) getLock(version).unlock()
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
        }
        if (lock) getLock(version).lock()
        try {
            val jar = getJar(version)
            if (!jar.exists()) download(version, true, lock = false)

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
            console?.let {
                val screen = console.createScreen("patch_${version.displayName}")
                ScreenProcessHandler(process, screen)
            }
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
            if (lock) getLock(version).unlock()
        }
    }

    override suspend fun patchCommand(
        type: ICloudServerVersionType,
        javaVersion: ICloudJavaVersion,
        jarToExecute: File
    ): List<String> {
        return IServerVersionHandler.getDefaultHandler().patchCommand(type, javaVersion, jarToExecute)
    }

    override fun getLock(version: ICloudServerVersion): ReentrantLock {
        return IServerVersionHandler.getDefaultHandler().getLock(version)
    }

}