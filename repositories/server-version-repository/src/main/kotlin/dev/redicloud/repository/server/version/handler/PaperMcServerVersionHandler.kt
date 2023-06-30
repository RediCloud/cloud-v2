package dev.redicloud.repository.server.version.handler

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.requester.PaperMcApiRequester
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.TEMP_SERVER_VERSION_FOLDER
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class PaperMcServerVersionHandler(
    val serverVersionRepository: ServerVersionRepository,
    override val type: CloudServerVersionType,
    override var lastUpdateCheck: Long = System.currentTimeMillis()
) : IServerVersionHandler {

    private val requester = PaperMcApiRequester(type)

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        val jar = getJar(version)
        if (jar.exists() && !force) return jar

        val buildId = requester.getLatestBuild(version.version)
        if (buildId == -1) throw NullPointerException("Cant find build for ${version.name}")

        val url = requester.getDownloadUrl(version.version, buildId)
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

        return jar
    }

    override suspend fun canDownload(version: CloudServerVersion): Boolean {
        val buildId = requester.getLatestBuild(version.version)
        if (buildId == -1) return false
        val url = requester.getDownloadUrl(version.version, buildId)
        val response = get(url)
        return response.statusCode == 200
    }

    override suspend fun isUpdateAvailable(version: CloudServerVersion, force: Boolean): Boolean {
        if (!force && System.currentTimeMillis() - lastUpdateCheck < 5.minutes.inWholeMilliseconds) return false
        val currentId = version.buildId ?: return true
        val latest = requester.getLatestBuild(version.version)
        if (latest == -1) throw NullPointerException("Cant find build for ${version.name}")
        lastUpdateCheck = System.currentTimeMillis()
        return latest > currentId.toInt()
    }

    override suspend fun getVersions(): List<ServerVersion> = requester.getVersions()

    override suspend fun getBuilds(version: ServerVersion): List<String> =
        requester.getBuilds(version).map { it.toString() }

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