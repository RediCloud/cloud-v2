package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.TEMP_SERVER_VERSION_FOLDER
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class URLServerVersionHandler(
    override val serverVersionRepository: CloudServerVersionRepository,
    override val nodeRepository: NodeRepository
) : IServerVersionHandler {

    override val name: String = "urldownloader"
    override var lastUpdateCheck: Long = -1
    private val locks = mutableMapOf<UUID, ReentrantLock>()

    override fun getLock(version: CloudServerVersion): ReentrantLock {
        return locks.getOrPut(version.uniqueId) { ReentrantLock() }
    }

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        val jar = getJar(version)
        if (jar.exists() && !force) return jar
        getLock(version).lock()
        try {
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
        val jar = getJar(version)
        if (!jar.exists()) download(version, true)

        getLock(version).lock()
        try {
            val versionDir = getFolder(version)
            val tempDir = File(UUID.randomUUID().toString(), TEMP_SERVER_VERSION_FOLDER.getFile().absolutePath)
            val tempJar = jar.copyTo(tempDir)

            val processBuilder = ProcessBuilder("java", "-jar", tempJar.absolutePath)
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
            File(versionDir, ".patched").createNewFile()
        }finally {
            getLock(version).unlock()
        }
    }
}