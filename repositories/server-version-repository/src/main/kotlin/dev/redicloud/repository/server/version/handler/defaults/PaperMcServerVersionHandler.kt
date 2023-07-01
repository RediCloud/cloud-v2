package dev.redicloud.repository.server.version.handler.defaults

import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.requester.PaperMcApiRequester
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.TEMP_SERVER_VERSION_FOLDER
import khttp.get
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class PaperMcServerVersionHandler(
    override val serverVersionRepository: CloudServerVersionRepository,
    val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    override val name: String = "papermc",
    override var lastUpdateCheck: Long = System.currentTimeMillis()
) : IServerVersionHandler {

    private val requester = PaperMcApiRequester()

    override suspend fun download(version: CloudServerVersion, force: Boolean): File {
        val jar = getJar(version)
        if (jar.exists() && !force) return jar

        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")

        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: throw NullPointerException("Cant find server version type ${version.typeId}")

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

        return jar
    }

    override suspend fun canDownload(version: CloudServerVersion): Boolean {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val buildId = requester.getLatestBuild(type, version.version)
        if (buildId == -1) return false
        val url = requester.getDownloadUrl(type, version.version, buildId)
        val response = get(url)
        return response.statusCode == 200
    }

    override suspend fun isUpdateAvailable(version: CloudServerVersion, force: Boolean): Boolean {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        if (!force && System.currentTimeMillis() - lastUpdateCheck < 5.minutes.inWholeMilliseconds) return false
        val currentId = version.buildId ?: return true
        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        val latest = requester.getLatestBuild(type, version.version)
        if (latest == -1) throw NullPointerException("Cant find build for ${version.getDisplayName()}")
        lastUpdateCheck = System.currentTimeMillis()
        return latest > currentId.toInt()
    }

    override suspend fun getVersions(version: CloudServerVersion): List<ServerVersion> {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getVersions(type)
    }

    override suspend fun getBuilds(version: CloudServerVersion, mcVersion: ServerVersion): List<String> {
        if (version.typeId == null) throw NullPointerException("Cant find server version type for ${version.getDisplayName()}")
        val type = serverVersionTypeRepository.getType(version.typeId!!) ?: throw NullPointerException("Cant find server version type ${version.typeId}")
        return requester.getBuilds(type, mcVersion).map { it.toString() }
    }


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