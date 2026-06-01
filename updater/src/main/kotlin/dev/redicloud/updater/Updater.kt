package dev.redicloud.updater

import dev.redicloud.api.commands.ICommandManager
import dev.redicloud.logging.LogManager
import dev.redicloud.updater.suggest.ChannelSuggester
import dev.redicloud.updater.suggest.VersionSuggester
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.jar.JarFile

@Suppress("TooManyFunctions")
object Updater {

    private const val GITHUB_API = "https://api.github.com/repos/RediCloud/cloud-v2/releases"
    private const val BUFFER_SIZE = 8192

    private val updateInfoFile = File(".update-info")

    /** The file to start after a version switch (set by [switchVersion]). */
    var updateToVersion: File? = null
        private set

    // ------------------------------------------------------------------
    // Startup
    // ------------------------------------------------------------------

    /** Called on startup to clean up after a previous version switch. */
    suspend fun check() {
        if (updateInfoFile.exists()) {
            cleanUpOldJars()
            updateInfoFile.delete()
        }
        val (available, release) = updateAvailable()
        if (available && release != null) {
            LogManager.rootLogger().info("Update available: ${release.version.display}")
            LogManager.rootLogger().info("  Download:  version download ${release.version.display}")
            LogManager.rootLogger().info("  Switch:    version switch ${release.version.display}")
        } else {
            LogManager.rootLogger().info("You are running the latest version!")
        }
    }

    fun registerSuggesters(commandManager: ICommandManager<*>) {
        commandManager.registerSuggesters(ChannelSuggester(), VersionSuggester())
    }

    // ------------------------------------------------------------------
    // GitHub Releases API
    // ------------------------------------------------------------------

    /** Fetches all non-draft releases from GitHub. */
    suspend fun getReleases(): List<ReleaseInfo> {
        val response = httpClient.get {
            url(GITHUB_API)
            header("Accept", "application/vnd.github+json")
        }
        if (!response.status.isSuccess()) return emptyList()
        val ghReleases = gson.fromJsonToList<GitHubRelease>(response.bodyAsText())
        return ghReleases
            .filterNot { it.draft }
            .mapNotNull { it.toReleaseInfo() }
    }

    /** Returns releases filtered by [channel]. */
    suspend fun getReleasesByChannel(channel: VersionChannel): List<ReleaseInfo> =
        getReleases().filter { it.channel == channel }.sortedBy { it.version }

    /** Returns the available channels that have at least one release. */
    suspend fun getAvailableChannels(): List<VersionChannel> =
        getReleases().map { it.channel }.distinct().sortedBy { it.order }

    // ------------------------------------------------------------------
    // Update check
    // ------------------------------------------------------------------

    /** Checks whether an update is available for the current channel. */
    suspend fun updateAvailable(): Pair<Boolean, ReleaseInfo?> {
        val current = CLOUD_VERSION_PARSED ?: return false to null
        if (BUILD == "local") return false to null

        val latest = getReleasesByChannel(current.channel).lastOrNull()
            ?: return false to null

        return (latest.version > current) to latest
    }

    // ------------------------------------------------------------------
    // Download & verify
    // ------------------------------------------------------------------

    /** Downloads a release zip into the `versions/` directory. */
    suspend fun download(release: ReleaseInfo): File {
        val zipUrl = release.zipUrl
            ?: error("Release ${release.version.display} has no zip asset")

        val versionsDir = File("versions").also { it.mkdirs() }
        val target = File(versionsDir, "redicloud-${release.version.display}.zip")

        val response = httpClient.get { url(zipUrl) }
        check(response.status.isSuccess()) { "Download failed: HTTP ${response.status}" }
        target.writeBytes(response.readRawBytes())

        // Verify checksum if available
        if (release.checksumsUrl != null) {
            verifyChecksum(target, release.checksumsUrl)
        }

        return target
    }

    /**
     * Downloads the checksum file and verifies the zip against it.
     *
     * @throws IllegalStateException if the checksum does not match.
     */
    private suspend fun verifyChecksum(zipFile: File, checksumsUrl: String) {
        val response = httpClient.get { url(checksumsUrl) }
        if (!response.status.isSuccess()) {
            LogManager.rootLogger().warning("Could not download checksums, skipping verification")
            return
        }
        val checksumLines = response.bodyAsText().lines()
        val expectedHash = checksumLines
            .firstOrNull { it.contains(zipFile.name) }
            ?.split("\\s+".toRegex())
            ?.firstOrNull()

        if (expectedHash == null) {
            LogManager.rootLogger().warning("No checksum found for ${zipFile.name}, skipping verification")
            return
        }

        val actualHash = sha256(zipFile)
        check(actualHash.equals(expectedHash, ignoreCase = true)) {
            "Checksum mismatch for ${zipFile.name}: expected $expectedHash, got $actualHash"
        }
        LogManager.rootLogger().info("Checksum verified for ${zipFile.name}")
    }

    // ------------------------------------------------------------------
    // Version switch
    // ------------------------------------------------------------------

    /** Extracts a downloaded version and writes the `.update-info` marker. */
    fun switchVersion(release: ReleaseInfo) {
        val versionsDir = File("versions")
        val zipFile = File(versionsDir, "redicloud-${release.version.display}.zip")
        check(zipFile.exists()) {
            "Version ${release.version.display} is not downloaded. Run: version download ${release.version.display}"
        }

        unzipFile(zipFile.absolutePath, File(".").absolutePath)

        // Find the freshly extracted node-service JAR
        updateToVersion = mainFolderJars().firstOrNull { jar ->
            val props = getJarProperties(jar)
            props["full-version"]?.let { CloudVersion.parseOrNull(it) } == release.version ||
                props["version"] == release.version.base
        } ?: error("Could not find extracted JAR for ${release.version.display}")

        val currentFull = CLOUD_VERSION_FULL
        val info = UpdateInfo(
            newVersion = release.version.full,
            oldVersion = currentFull
        )
        updateInfoFile.writeText(gson.toJson(info))
    }

    // ------------------------------------------------------------------
    // Local versions
    // ------------------------------------------------------------------

    /** Lists locally downloaded versions grouped by channel. */
    fun localInstalledVersions(): Map<VersionChannel, List<CloudVersion>> {
        val versionsDir = File("versions")
        if (!versionsDir.exists()) return emptyMap()

        return versionsDir.listFiles()
            ?.filter { it.extension == "zip" }
            ?.mapNotNull { file ->
                val name = file.nameWithoutExtension.removePrefix("redicloud-")
                CloudVersion.parseOrNull(name)
            }
            ?.groupBy { it.channel }
            ?: emptyMap()
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun cleanUpOldJars() {
        val currentVersion = CLOUD_VERSION_FULL
        mainFolderJars()
            .map { it to getJarProperties(it) }
            .filter { (_, props) -> props.isNotEmpty() }
            .filterNot { (_, props) -> props["full-version"] == currentVersion }
            .forEach { (file, _) -> file.delete() }
    }

    private fun getJarProperties(file: File): Map<String, String> {
        if (!file.exists() || file.extension != "jar") return emptyMap()
        return runCatching {
            JarFile(file).use { jar ->
                jar.getJarEntry("redicloud-version.properties")?.let { entry ->
                    jar.getInputStream(entry).use { stream ->
                        val props = Properties()
                        props.load(stream)
                        props.entries.associate { it.key.toString() to it.value.toString() }
                    }
                } ?: emptyMap()
            }
        }.getOrDefault(emptyMap())
    }

    private fun mainFolderJars(): List<File> =
        File(".").listFiles()?.filter { it.extension == "jar" } ?: emptyList()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Converts a GitHub release to our domain model, or `null` if the tag is unparseable. */
    private fun GitHubRelease.toReleaseInfo(): ReleaseInfo? {
        val versionStr = tagName.removePrefix("v")
        val version = CloudVersion.parseOrNull(versionStr) ?: return null

        return ReleaseInfo(
            version = version,
            tagName = tagName,
            zipUrl = assets.firstOrNull { it.name.endsWith(".zip") }?.downloadUrl,
            checksumsUrl = assets.firstOrNull { it.name == "checksums.sha256" }?.downloadUrl,
            signatureUrl = assets.firstOrNull { it.name == "checksums.sha256.sig" }?.downloadUrl
        )
    }
}
