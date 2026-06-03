package dev.redicloud.service.node

import dev.redicloud.logging.LogManager
import dev.redicloud.updater.ReleaseInfo
import dev.redicloud.updater.Updater
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel

/**
 * Central upgrade logic shared by the interactive `version upgrade` command,
 * the [ClusterUpgradePacket] handler on remote nodes, and the automatic
 * migration-driven upgrade in [NodeService].
 *
 * All three callers need the same "resolve → download → install" pipeline;
 * this object removes that duplication.
 */
object UpgradeOrchestrator {

    private val LOGGER = LogManager.logger(UpgradeOrchestrator::class)

    /** Result of a single-node upgrade attempt. */
    sealed class Result {
        data class Success(val release: ReleaseInfo) : Result()
        data class ReleaseNotFound(val version: String, val channel: String) : Result()
        data class Failed(val message: String, val cause: Exception? = null) : Result()
    }

    /**
     * Resolves the release for the given [version] on [channel], downloads it,
     * verifies integrity and switches the local installation.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun upgradeLocal(channel: VersionChannel, version: CloudVersion): Result {
        val release = resolveRelease(channel, version)
            ?: return Result.ReleaseNotFound(version.display, channel.label)

        return upgradeLocal(release)
    }

    /**
     * Downloads the given [release], verifies integrity and switches the local installation.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun upgradeLocal(release: ReleaseInfo): Result {
        return try {
            LOGGER.info("Downloading version ${release.version.display}...")
            Updater.download(release)

            LOGGER.info("Installing version ${release.version.display}...")
            Updater.switchVersion(release)

            LOGGER.info("Upgrade to ${release.version.display} complete. Changes will be applied on next restart.")
            Result.Success(release)
        } catch (e: Exception) {
            LOGGER.severe("Upgrade to ${release.version.display} failed", e)
            Result.Failed(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Looks up the matching [ReleaseInfo] from GitHub for the given [channel] and [version].
     * Returns `null` if no matching release is found.
     */
    suspend fun resolveRelease(channel: VersionChannel, version: CloudVersion): ReleaseInfo? {
        val releases = Updater.getReleasesByChannel(channel)
        return releases.firstOrNull { it.version == version }
    }
}
