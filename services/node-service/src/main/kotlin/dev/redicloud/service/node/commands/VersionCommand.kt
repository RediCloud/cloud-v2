package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNode
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.service.node.packets.upgrade.ClusterUpgradePacket
import dev.redicloud.service.node.packets.upgrade.ClusterUpgradeResponsePacket
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.updater.ReleaseInfo
import dev.redicloud.updater.Updater
import dev.redicloud.updater.suggest.ChannelSuggester
import dev.redicloud.updater.suggest.VersionSuggester
import dev.redicloud.utils.*
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Command("version")
@CommandAlias(["ver"])
@CommandDescription("Displays the current version of the node service")
class VersionCommand(
    private val console: Console,
    private val serviceId: ServiceId,
    private val nodeRepository: ICloudNodeRepository,
    private val packetManager: IPacketManager
) : ICommand {

    companion object {
        private const val ANIMATION_TICK_MS = 200L
        private const val UPGRADE_CONFIRM_TIMEOUT_MS = 30000
        private val CLUSTER_UPGRADE_TIMEOUT = 120.seconds
    }

    @CommandSubPath("")
    @CommandDescription("Displays the current version of the node service")
    fun version(
        actor: ConsoleActor
    ) {
        actor.sendHeader("Version")
        actor.sendMessage("Version§8: %hc%$CLOUD_VERSION_FULL")
        actor.sendMessage("Channel§8: %hc%$CLOUD_VERSION_CHANNEL")
        actor.sendMessage("Git§8: %hc%$GIT")
        actor.sendMessage("Branch§8: %hc%$BRANCH")
        actor.sendMessage("CI-Build§8: %hc%$BUILD")
        actor.sendHeader("Version")
    }

    @CommandSubPath("checkupdate")
    @CommandDescription("Checks if an update is available")
    suspend fun checkUpdate(
        actor: ConsoleActor
    ) {
        if (BUILD == "local") {
            actor.sendMessage("You are running a local build, updates are not available!")
            return
        }
        val (available, release) = Updater.updateAvailable()
        if (available && release != null) {
            actor.sendMessage(
                "An update is available: %hc%${release.version.display}"
            )
            actor.sendMessage(
                "Upgrade: %hc%version upgrade ${release.channel.label} ${release.version.display}"
            )
        } else {
            actor.sendMessage("You are running the latest version!")
        }
    }

    private val upgradeConfirms = mutableMapOf<String, Long>()

    @CommandSubPath("upgrade [channel] [version]")
    @CommandDescription("Downloads and installs a version upgrade on all cluster nodes")
    @Suppress("ReturnCount", "LongMethod")
    suspend fun upgrade(
        actor: ConsoleActor,
        @CommandParameter("channel", false, ChannelSuggester::class) channelParam: String?,
        @CommandParameter("version", false, VersionSuggester::class) versionParam: String?
    ) {
        if (Updater.updateToVersion != null) {
            actor.sendMessage("§cAn upgrade is already pending! Restart the node service to apply the changes!")
            return
        }

        val channel = channelParam?.let { VersionChannel.fromLabelOrNull(it) }
            ?: CLOUD_VERSION_PARSED?.channel
            ?: VersionChannel.STABLE

        val release = resolveRelease(actor, channel, versionParam) ?: return
        val current = CLOUD_VERSION_PARSED

        if (current != null && release.version == current) {
            actor.sendMessage("You are already running this version!")
            return
        }

        // Confirm channel change
        val confirmKey = release.version.display
        if (current != null && release.channel != current.channel &&
            upgradeConfirms.getOrDefault(confirmKey, 0) + UPGRADE_CONFIRM_TIMEOUT_MS < System.currentTimeMillis()
        ) {
            actor.sendMessage("§cYou are switching to a different channel (${channel.label})!")
            actor.sendMessage("§cThis can cause issues. Backup your data first!")
            actor.sendMessage("§cType the command again to confirm!")
            upgradeConfirms[confirmKey] = System.currentTimeMillis()
            return
        }

        // Confirm downgrade
        if (current != null && release.version < current &&
            upgradeConfirms.getOrDefault(confirmKey, 0) + UPGRADE_CONFIRM_TIMEOUT_MS < System.currentTimeMillis()
        ) {
            actor.sendMessage("§cYou are downgrading to ${release.version.display}!")
            actor.sendMessage("§cThis can cause issues and data loss!")
            actor.sendMessage("§cType the command again to confirm!")
            upgradeConfirms[confirmKey] = System.currentTimeMillis()
            return
        }
        upgradeConfirms.remove(confirmKey)

        // Collect cluster nodes
        val allNodes = nodeRepository.getConnectedNodes()
        val otherNodes = allNodes.filter { it.serviceId != serviceId }

        // Build status tracker: nodeId -> status string
        val nodeStatuses = ConcurrentHashMap<ServiceId, String>()
        nodeStatuses[serviceId] = "§epending"
        otherNodes.forEach { nodeStatuses[it.serviceId] = "§epending" }

        // Pre-upgrade overview
        actor.sendHeader("Cluster Upgrade")
        actor.sendMessage("Upgrading cluster to %hc%${release.version.display}§8:")

        // Start live-status animations (one per node)
        val animationsDone = ConcurrentHashMap<ServiceId, Boolean>()

        fun createNodeAnimation(node: ICloudNode, isThis: Boolean): AnimatedLineAnimation {
            val label = if (isThis) "${node.name} §7(§athis§7)" else node.name
            return AnimatedLineAnimation(console, ANIMATION_TICK_MS) {
                val status = nodeStatuses[node.serviceId] ?: "§7unknown"
                if (animationsDone[node.serviceId] == true) {
                    null
                } else if (status.contains("pending") || status.contains("downloading")) {
                    "  §8- %hc%$label §8: $status %loading%"
                } else {
                    animationsDone[node.serviceId] = true
                    "  §8- %hc%$label §8: $status"
                }
            }
        }

        // This node first
        val thisNode = allNodes.first { it.serviceId == serviceId }
        console.startAnimation(createNodeAnimation(thisNode, true))

        // Other nodes
        otherNodes.forEach { node ->
            console.startAnimation(createNodeAnimation(node, false))
        }

        // Phase 1: Local upgrade
        nodeStatuses[serviceId] = "§edownloading"
        @Suppress("TooGenericExceptionCaught")
        try {
            Updater.download(release)
            Updater.switchVersion(release)
            nodeStatuses[serviceId] = "§aupgraded"
        } catch (e: Exception) {
            nodeStatuses[serviceId] = "§cfailed §8(${e.message})"
            LOGGER.severe("Failed to upgrade local node", e)
            delay((ANIMATION_TICK_MS * 2).milliseconds)
            actor.sendMessage("")
            actor.sendMessage("§cLocal upgrade failed! Aborting cluster upgrade.")
            actor.sendHeader("Cluster Upgrade")
            return
        }

        // Phase 2: Remote upgrade
        if (otherNodes.isNotEmpty()) {
            otherNodes.forEach { nodeStatuses[it.serviceId] = "§edownloading" }

            val receiverIds = otherNodes.map { it.serviceId }.toTypedArray()
            val packet = ClusterUpgradePacket(
                targetVersion = release.version.display,
                targetChannel = channel.label
            )

            packetManager.publish(packet, *receiverIds)
                .withTimeOut(CLUSTER_UPGRADE_TIMEOUT)
                .waitForResponse(otherNodes.size) { response: AbstractPacket? ->
                    if (response is ClusterUpgradeResponsePacket) {
                        val senderId = response.sender ?: return@waitForResponse
                        if (response.success) {
                            nodeStatuses[senderId] = "§aupgraded"
                        } else {
                            nodeStatuses[senderId] = "§cfailed §8(${response.errorMessage ?: "unknown"})"
                        }
                    }
                }
        }

        // Wait for animations to finish rendering
        delay((ANIMATION_TICK_MS * 2).milliseconds)

        // Summary
        actor.sendMessage("")
        val allSuccess = nodeStatuses.values.all { it.contains("upgraded") }
        if (!allSuccess) {
            actor.sendMessage("§cNot all nodes were upgraded successfully!")
        }
        actor.sendMessage("§eThe entire cluster must be shut down for the upgrade to take effect.")
        actor.sendMessage("§eMigrations will run automatically on next startup.")
        actor.sendHeader("Cluster Upgrade")
    }

    @CommandSubPath("channels")
    @CommandDescription("Displays all available release channels")
    suspend fun channels(
        actor: ConsoleActor
    ) {
        val channels = Updater.getAvailableChannels()
        if (channels.isEmpty()) {
            actor.sendMessage("§cFailed to get available channels!")
            return
        }
        val currentChannel = CLOUD_VERSION_PARSED?.channel
        actor.sendMessage("Available channels:")
        channels.forEach { ch ->
            if (ch == currentChannel) {
                actor.sendMessage("§8- %hc%${ch.label} §7(§acurrent§7)")
            } else {
                actor.sendMessage("§8- %hc%${ch.label}")
            }
        }
    }

    @CommandSubPath("releases [channel]")
    @CommandDescription("Displays all available releases for a channel")
    suspend fun releases(
        actor: ConsoleActor,
        @CommandParameter("channel", false, ChannelSuggester::class) channelParam: String?
    ) {
        val channel = channelParam?.let { VersionChannel.fromLabelOrNull(it) }
            ?: CLOUD_VERSION_PARSED?.channel
            ?: VersionChannel.STABLE

        val releases = Updater.getReleasesByChannel(channel)
        if (releases.isEmpty()) {
            actor.sendMessage("§cNo releases found for channel ${toConsoleValue(channel.label)}!")
            return
        }
        val current = CLOUD_VERSION_PARSED
        actor.sendMessage("Available releases for ${toConsoleValue(channel.label)}:")
        releases.forEach { rel ->
            if (current != null && rel.version == current) {
                actor.sendMessage("§8- %hc%${rel.version.display} §7(§acurrent§7)")
            } else {
                actor.sendMessage("§8- %hc%${rel.version.display}")
            }
        }
    }

    @CommandSubPath("downloaded")
    @CommandDescription("Displays all downloaded versions")
    fun downloaded(
        actor: ConsoleActor
    ) {
        val installedVersions = Updater.localInstalledVersions()
        if (installedVersions.isEmpty()) {
            actor.sendMessage("No versions downloaded!")
            actor.sendMessage("Upgrade with: ${toConsoleValue("version upgrade [channel] [version]")}")
            return
        }
        val current = CLOUD_VERSION_PARSED
        actor.sendMessage("Downloaded versions:")
        installedVersions.forEach { (channel, versions) ->
            actor.sendMessage("§8- %hc%${channel.label}:")
            versions.forEach { ver ->
                if (current != null && ver == current) {
                    actor.sendMessage("  §8- %tc%${ver.display} §7(§acurrent§7)")
                } else {
                    actor.sendMessage("  §8- %tc%${ver.display}")
                }
            }
        }
    }

    /** Resolves a [ReleaseInfo] from user input, handling "latest" and explicit version strings. */
    private suspend fun resolveRelease(
        actor: ConsoleActor,
        channel: VersionChannel,
        versionParam: String?
    ): ReleaseInfo? {
        if (versionParam == null || versionParam == "latest") {
            val releases = Updater.getReleasesByChannel(channel)
            if (releases.isEmpty()) {
                actor.sendMessage("§cNo releases found for channel ${toConsoleValue(channel.label)}!")
                return null
            }
            return releases.last()
        }

        val targetVersion = CloudVersion.parseOrNull(versionParam)
        if (targetVersion == null) {
            actor.sendMessage("§cInvalid version format: ${toConsoleValue(versionParam, false)}")
            return null
        }

        val releases = Updater.getReleasesByChannel(channel)
        return releases.firstOrNull { it.version == targetVersion } ?: run {
            actor.sendMessage("§cVersion ${toConsoleValue(versionParam, false)} not found!")
            null
        }
    }
}
