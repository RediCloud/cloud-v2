package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.requester.PaperMcApiRequester
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.TEMP_SERVER_VERSION_FOLDER
import dev.redicloud.utils.findFreePort
import dev.redicloud.utils.isValidUrl
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class PaperMcServerVersionHandler(
    override val serverVersionRepository: CloudServerVersionRepository,
    val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    val javaVersionRepository: JavaVersionRepository,
    override val nodeRepository: NodeRepository,
    val console: Console,
    override val name: String = "papermc",
    override var lastUpdateCheck: Long = System.currentTimeMillis()
) : IServerVersionHandler {

    private val requester = PaperMcApiRequester()
    private val locks = mutableMapOf<UUID, ReentrantLock>()

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        val jar = getJar(version)
        if (jar.exists() && !force) return jar
        locks.getOrDefault(version.uniqueId, ReentrantLock()).lock()
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
                "Downloading version %hc%${version.getDisplayName()}§8: %hc%%loading%"
            }
        }
        console.startAnimation(animation)
        try {
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
            lastUpdateCheck = System.currentTimeMillis()
        }catch (e: Exception) {
            error = true
            throw e
        } finally {
            downloaded = true
            locks[version.uniqueId]?.unlock()
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

    override suspend fun patch(version: CloudServerVersion) {
        locks.getOrDefault(version.uniqueId, ReentrantLock()).lock()
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
        try {
            val jar = getJar(version)
            if (!jar.exists()) download(version, true)

            val versionDir = getFolder(version)
            val tempDir = File(TEMP_SERVER_VERSION_FOLDER.getFile().absolutePath, UUID.randomUUID().toString())
            tempDir.mkdirs()
            val tempJar = jar.copyTo(File(tempDir, jar.name))

            if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
            val type = serverVersionTypeRepository.getType(version.typeId!!)
                ?: throw NullPointerException("Cant find server version type ${version.typeId}")
            if (version.javaVersionId == null) throw NullPointerException("Cant find java version for ${version.getDisplayName()}")
            val javaVersion = javaVersionRepository.getVersion(version.javaVersionId!!)
                ?: throw NullPointerException("Cant find java version for ${version.getDisplayName()}")
            val port = findFreePort(40000..60000)

            val processBuilder = ProcessBuilder(patchCommand(type, javaVersion, tempJar))
            processBuilder.directory(tempDir)
            processBuilder.start().waitFor(5.minutes.inWholeMilliseconds, TimeUnit.MILLISECONDS)

            if (!versionDir.exists()) versionDir.mkdirs()

            tempJar.copyTo(jar, true)
            if (version.libPattern != null) {
                val pattern = Pattern.compile(version.libPattern!!)
                tempDir.listFiles()?.forEach {
                    if (!pattern.matcher(it.name).find()) {
                        if (it.isDirectory) {
                            it.deleteRecursively()
                        } else {
                            it.delete()
                        }
                        return@forEach
                    }
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
            locks[version.uniqueId]?.unlock()
        }
    }


}