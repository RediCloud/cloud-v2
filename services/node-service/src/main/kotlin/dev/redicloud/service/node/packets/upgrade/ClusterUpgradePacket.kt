package dev.redicloud.service.node.packets.upgrade

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.UpgradeOrchestrator
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel

class ClusterUpgradePacket(
    val targetVersion: String,
    val targetChannel: String
) : AbstractPacket() {

    companion object {
        private val LOGGER = LogManager.logger(ClusterUpgradePacket::class)
    }

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

        LOGGER.info("Received cluster upgrade request to $targetVersion ($targetChannel)")

        when (val result = UpgradeOrchestrator.upgradeLocal(channel, version)) {
            is UpgradeOrchestrator.Result.Success ->
                respond(ClusterUpgradeResponsePacket(true))
            is UpgradeOrchestrator.Result.ReleaseNotFound ->
                respond(ClusterUpgradeResponsePacket(false, "Release not found: $targetVersion"))
            is UpgradeOrchestrator.Result.Failed ->
                respond(ClusterUpgradeResponsePacket(false, result.message))
        }
    }
}
