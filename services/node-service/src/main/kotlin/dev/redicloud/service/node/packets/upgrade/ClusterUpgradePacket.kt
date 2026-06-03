package dev.redicloud.service.node.packets.upgrade

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.logging.LogManager
import dev.redicloud.updater.Updater
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel

class ClusterUpgradePacket(
    val targetVersion: String,
    val targetChannel: String
) : AbstractPacket() {

    companion object {
        private val LOGGER = LogManager.logger(ClusterUpgradePacket::class)
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun received(manager: IPacketManager) {
        super.received(manager)

        val channel = VersionChannel.fromLabelOrNull(targetChannel)
        if (channel == null) {
            respond(ClusterUpgradeResponsePacket(false, "Unknown channel: $targetChannel"))
            return
        }

        val version = CloudVersion.parseOrNull(targetVersion)
        if (version == null) {
            respond(ClusterUpgradeResponsePacket(false, "Invalid version: $targetVersion"))
            return
        }

        try {
            LOGGER.info("Received cluster upgrade request to $targetVersion ($targetChannel)")

            // Find the release
            val releases = Updater.getReleasesByChannel(channel)
            val release = releases.firstOrNull { it.version == version }
            if (release == null) {
                respond(ClusterUpgradeResponsePacket(false, "Release not found: $targetVersion"))
                return
            }

            // Download + verify
            LOGGER.info("Downloading version $targetVersion...")
            Updater.download(release)

            // Switch version
            LOGGER.info("Installing version $targetVersion...")
            Updater.switchVersion(release)

            LOGGER.info("Upgrade to $targetVersion complete. Changes will be applied on next restart.")
            respond(ClusterUpgradeResponsePacket(true))
        } catch (e: Exception) {
            LOGGER.severe("Failed to upgrade to $targetVersion", e)
            respond(ClusterUpgradeResponsePacket(false, e.message ?: "Unknown error"))
        }
    }
}
