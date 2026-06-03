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
import dev.redicloud.service.node.UpgradeOrchestrator
import dev.redicloud.service.node.packets.upgrade.ClusterUpgradePacket
import dev.redicloud.service.node.packets.upgrade.ClusterUpgradeResponsePacket
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
    suspend fun upgrade(
        actor: ConsoleActor,
        @CommandParameter("channel", false, ChannelSuggester::class) channelParam: String?,
        @CommandParameter("version", false, VersionSuggester::class) versionParam: String?
    ) {
        if (Updater.updateToVersion != null) {
            actor.sendMessage("§cAn upgrade is already pending! Restart the node service to apply the changes!")
            return
        }

        val channel = resolveChannel(channelParam)
        val release = resolveRelease(actor, channel, versionParam) ?: return
        val current = CLOUD_VERSION_PARSED

        if (current != null && release.version == current) {
            actor.sendMessage("You are already running this version!")
            return
        }

        if (requiresConfirmation(actor, current, release, channel)) return

        val allNodes = nodeRepository.getConnectedNodes()
        val otherNodes = allNodes.filter { it.serviceId != serviceId }

        val nodeStatuses = initNodeStatuses(otherNodes)
        actor.sendHeader("Cluster Upgrade")
        actor.sendMessage("Upgrading cluster to %hc%${release.version.display}§8:")

        val animationsDone = ConcurrentHashMap<ServiceId, Boolean>()
        startNodeAnimations(allNodes, otherNodes, nodeStatuses, animationsDone)

        if (!performLocalUpgrade(actor, release, nodeStatuses)) return

        performRemoteUpgrade(otherNodes, nodeStatuses, release, channel)

        delay((ANIMATION_TICK_MS * 2).milliseconds)
        printUpgradeSummary(actor, nodeStatuses)
    }

    private fun resolveChannel(channelParam: String?): VersionChannel =
        channelParam?.let { VersionChannel.fromLabelOrNull(it) }
            ?: CLOUD_VERSION_PARSED?.channel
            ?: VersionChannel.STABLE

    /**
     * Checks whether a dangerous operation (channel switch or downgrade) needs user confirmation.
     * Returns `true` if the command should abort and wait for the user to re-run it.
     */
    private fun requiresConfirmation(
        actor: ConsoleActor,
        current: CloudVersion?,
        release: ReleaseInfo,
        channel: VersionChannel
    ): Boolean {
        if (current == null) return false
        val confirmKey = release.version.display
        val lastConfirm = upgradeConfirms.getOrDefault(confirmKey, 0)
        val expired = lastConfirm + UPGRADE_CONFIRM_TIMEOUT_MS < System.currentTimeMillis()

        if (release.channel != current.channel && expired) {
            actor.sendMessage("§cYou are switching to a different channel (${channel.label})!")
            actor.sendMessage("§cThis can cause issues. Backup your data first!")
            actor.sendMessage("§cType the command again to confirm!")
            upgradeConfirms[confirmKey] = System.currentTimeMillis()
            return true
        }

        if (release.version < current && expired) {
            actor.sendMessage("§cYou are downgrading to ${release.version.display}!")
            actor.sendMessage("§cThis can cause issues and data loss!")
            actor.sendMessage("§cType the command again to confirm!")
            upgradeConfirms[confirmKey] = System.currentTimeMillis()
            return true
        }

        upgradeConfirms.remove(confirmKey)
        return false
    }

    private fun initNodeStatuses(otherNodes: List<ICloudNode>): ConcurrentHashMap<ServiceId, String> {
        val statuses = ConcurrentHashMap<ServiceId, String>()
        statuses[serviceId] = "§epending"
        otherNodes.forEach { statuses[it.serviceId] = "§epending" }
        return statuses
    }

    private fun startNodeAnimations(
        allNodes: List<ICloudNode>,
        otherNodes: List<ICloudNode>,
        nodeStatuses: ConcurrentHashMap<ServiceId, String>,
        animationsDone: ConcurrentHashMap<ServiceId, Boolean>
    ) {
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

        val thisNode = allNodes.first { it.serviceId == serviceId }
        console.startAnimation(createNodeAnimation(thisNode, true))
        otherNodes.forEach { node ->
            console.startAnimation(createNodeAnimation(node, false))
        }
    }

    /**
     * Downloads and applies the upgrade on this node.
     * @return `true` if successful, `false` if the upgrade failed (caller should abort).
     */
    private suspend fun performLocalUpgrade(
        actor: ConsoleActor,
        release: ReleaseInfo,
        nodeStatuses: ConcurrentHashMap<ServiceId, String>
    ): Boolean {
        nodeStatuses[serviceId] = "§edownloading"
        return when (val result = UpgradeOrchestrator.upgradeLocal(release)) {
            is UpgradeOrchestrator.Result.Success -> {
                nodeStatuses[serviceId] = "§aupgraded"
                true
            }
            is UpgradeOrchestrator.Result.Failed -> {
                nodeStatuses[serviceId] = "§cfailed §8(${result.message})"
                delay((ANIMATION_TICK_MS * 2).milliseconds)
                actor.sendMessage("")
                actor.sendMessage("§cLocal upgrade failed! Aborting cluster upgrade.")
                actor.sendHeader("Cluster Upgrade")
                false
            }
            is UpgradeOrchestrator.Result.ReleaseNotFound -> {
                nodeStatuses[serviceId] = "§cfailed §8(release not found)"
                delay((ANIMATION_TICK_MS * 2).milliseconds)
                actor.sendMessage("")
                actor.sendMessage("§cRelease not found! Aborting cluster upgrade.")
                actor.sendHeader("Cluster Upgrade")
                false
            }
        }
    }

    private suspend fun performRemoteUpgrade(
        otherNodes: List<ICloudNode>,
        nodeStatuses: ConcurrentHashMap<ServiceId, String>,
        release: ReleaseInfo,
        channel: VersionChannel
    ) {
        if (otherNodes.isEmpty()) return
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

    private fun printUpgradeSummary(
        actor: ConsoleActor,
        nodeStatuses: ConcurrentHashMap<ServiceId, String>
    ) {
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
