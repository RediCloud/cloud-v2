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
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.*
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

open class URLServerVersionHandler(
    override val serverVersionRepository: CloudServerVersionRepository,
    override val nodeRepository: NodeRepository,
    override val console: Console,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    private val javaVersionRepository: JavaVersionRepository
) : IServerVersionHandler {

    companion object {
        private val logger = LogManager.logger(URLServerVersionHandler::class)
    }

    override val name: String = "urldownloader"
    override var lastUpdateCheck: Long = -1
    private val locks = mutableMapOf<UUID, ReentrantLock>()

    override fun getLock(version: CloudServerVersion): ReentrantLock {
        return locks.getOrPut(version.uniqueId) { ReentrantLock() }
    }

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        getLock(version).lock()
        val jar = getJar(version)
        try {
            if (jar.exists() && !force) return jar
            if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")

            val type = serverVersionTypeRepository.getType(version.typeId!!)
                ?: throw NullPointerException("Cant find server version type ${version.typeId}")
            if (version.customDownloadUrl == null) throw NullPointerException("Download url of ${version.getDisplayName()} is null")

            val response = get(version.customDownloadUrl!!)
            if (response.statusCode != 200) throw IllegalStateException(
                "Download of ${version.getDisplayName()} is not available (${response.statusCode}):\n" +
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
                    val url1 = it.key
                    val path = it.value
                    try {
                        if (!isValidUrl(url1)) {
                            logger.warning("§cInvalid default file with url ${toConsoleValue(url1, false)} for ${toConsoleValue(version.getDisplayName(), false)}")
                            return@add
                        }
                        val file = File(folder, path)
                        if (!file.parentFile.exists()) file.parentFile.mkdirs()
                        val response1 = get(url1)
                        if (response1.statusCode != 200) {
                            logger.warning("§cDownload of default file ${toConsoleValue(url1, false)} for ${toConsoleValue(version.getDisplayName(), false)} is not available (${response.statusCode}):\n${response.text}")
                            return@add
                        }
                        file.writeBytes(response1.content)
                    }catch (e: Exception) {
                        logger.warning("§cFailed to download default file ${toConsoleValue(url1, false)} for ${toConsoleValue(version.getDisplayName(), false)}", e)
                    }
                }
            }

            downloader.joinAll()
        }finally {
            getLock(version).unlock()
        }
        return jar
    }

    override suspend fun canDownload(version: CloudServerVersion): Boolean {
        return version.customDownloadUrl != null && get(version.customDownloadUrl!!).statusCode == 200
    }

    override suspend fun isUpdateAvailable(version: CloudServerVersion, force: Boolean): Boolean {
        return false
    }

    override suspend fun getVersions(version: CloudServerVersion): List<ServerVersion> = emptyList()

    override suspend fun getBuilds(version: CloudServerVersion, mcVersion: ServerVersion): List<String> = emptyList()

    override suspend fun update(version: CloudServerVersion): File {
        download(version, true)
        if (isPatchVersion(version)) patch(version)
        return getFolder(version)
    }

    override suspend fun patch(version: CloudServerVersion) {
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
                "Patching version %tc%${toConsoleValue(version.getDisplayName())}§8: ${if (error) "§4✘" else "§2✓"}"
            } else {
                "Patching version %tc%${toConsoleValue(version.getDisplayName())}§8: %tc%%loading%"
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

            if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
            val type = serverVersionTypeRepository.getType(version.typeId!!)
                ?: throw NullPointerException("Cant find server version type ${version.typeId}")
            if (version.javaVersionId == null) throw NullPointerException("Cant find java version for ${version.getDisplayName()}")
            val javaVersion = javaVersionRepository.getVersion(version.javaVersionId!!)
                ?: throw NullPointerException("Cant find java version for ${version.getDisplayName()}")
            findFreePort(40000..60000)

            val processBuilder = ProcessBuilder(patchCommand(type, javaVersion, tempJar))
            processBuilder.directory(tempDir)
            processBuilder.start().waitFor(5.minutes.inWholeMilliseconds, TimeUnit.MILLISECONDS)

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