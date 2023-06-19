package dev.redicloud.repository.server.version.handler

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.TEMP_SERVER_VERSION_FOLDER
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class ServerVersionHandler(override val type: CloudServerVersionType) : IServerVersionHandler {

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        val jar = getJar(version)
        if (jar.exists() && !force) return jar
        if (version.customDownloadUrl == null) throw NullPointerException("Download url of ${version.name} is null")

        val response = get(version.customDownloadUrl!!)
        if (response.statusCode != 200) throw IllegalStateException(
            "Download of ${version.name} is not available (${response.statusCode}):\n" +
                    response.text
        )

        val folder = getFolder(version)
        if (folder.exists()) folder.deleteRecursively()
        folder.mkdirs()
        if (jar.exists()) jar.delete()
        jar.writeBytes(response.content)
        return jar
    }

    override suspend fun isUpdateAvailable(version: CloudServerVersion): Boolean = false

    override suspend fun getVersions(): List<ServerVersion> = emptyList()

    override suspend fun getBuilds(version: ServerVersion): List<String> = emptyList()

    override suspend fun update(version: CloudServerVersion): File {
        download(version, true)
        if (isPatchVersion(version)) patch(version)
        return getFolder(version)
    }

    override suspend fun patch(version: CloudServerVersion) {
        val jar = getJar(version)
        if (!jar.exists()) download(version, true)

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
    }
}