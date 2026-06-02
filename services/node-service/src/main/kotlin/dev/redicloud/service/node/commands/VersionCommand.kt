package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.updater.ReleaseInfo
import dev.redicloud.updater.Updater
import dev.redicloud.updater.suggest.ChannelSuggester
import dev.redicloud.updater.suggest.VersionSuggester
import dev.redicloud.utils.*
import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel

@Command("version")
@CommandAlias(["ver"])
@CommandDescription("Displays the current version of the node service")
class VersionCommand(
    val console: NodeConsole
) : ICommand {

    companion object {
        private const val ANIMATION_TICK_MS = 200L
        private const val SWITCH_CONFIRM_TIMEOUT_MS = 30000
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
                "Download: %hc%version download ${release.channel.label} ${release.version.display}"
            )
            actor.sendMessage(
                "Switch:   %hc%version switch ${release.channel.label} ${release.version.display}"
            )
        } else {
            actor.sendMessage("You are running the latest version!")
        }
    }

    @CommandSubPath("download [channel] [version]")
    @CommandDescription("Downloads a version")
    suspend fun download(
        actor: ConsoleActor,
        @CommandParameter("channel", false, ChannelSuggester::class) channelParam: String?,
        @CommandParameter("version", false, VersionSuggester::class) versionParam: String?
    ) {
        val channel = channelParam?.let { VersionChannel.fromLabelOrNull(it) }
            ?: CLOUD_VERSION_PARSED?.channel
            ?: VersionChannel.STABLE

        val release = resolveRelease(actor, channel, versionParam) ?: return

        var canceled = false
        var error = false
        var downloaded = false
        val animation = AnimatedLineAnimation(
            console,
            ANIMATION_TICK_MS
        ) {
            if (canceled) {
                null
            } else if (downloaded) {
                canceled = true
                "Downloaded ${toConsoleValue(release.version.display)}§8: ${if (error) "§4x" else "§2ok"}"
            } else {
                "Downloading ${toConsoleValue(release.version.display)}§8: %loading%"
            }
        }
        console.startAnimation(animation)
        @Suppress("TooGenericExceptionCaught")
        try {
            Updater.download(release)
            downloaded = true
            actor.sendMessage(
                "Switch with: %hc%version switch ${channel.label} ${release.version.display}"
            )
        } catch (e: Exception) {
            error = true
            downloaded = true
            actor.sendMessage("§cFailed to download the version!")
            LOGGER.severe("Failed to download the version", e)
        }
    }

    private val switchConfirms = mutableMapOf<String, Long>()

    @CommandSubPath("switch [channel] [version]")
    @CommandDescription("Switch to a downloaded version")
    @Suppress("ReturnCount")
    suspend fun switch(
        actor: ConsoleActor,
        @CommandParameter("channel", false, ChannelSuggester::class) channelParam: String?,
        @CommandParameter("version", false, VersionSuggester::class) versionParam: String?
    ) {
        if (Updater.updateToVersion != null) {
            actor.sendMessage("§cAn update was already installed! Restart the node service to apply the changes!")
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

        // Check if downloaded
        val installed = Updater.localInstalledVersions()
        val channelVersions = installed[channel] ?: emptyList()
        if (release.version !in channelVersions) {
            actor.sendMessage("§cVersion not downloaded!")
            actor.sendMessage("§cDownload with: %hc%version download ${channel.label} ${release.version.display}")
            return
        }

        // Confirm channel change
        val confirmKey = release.version.display
        if (current != null && release.channel != current.channel &&
            switchConfirms.getOrDefault(confirmKey, 0) + SWITCH_CONFIRM_TIMEOUT_MS < System.currentTimeMillis()
        ) {
            actor.sendMessage("§cYou are switching to a different channel (${channel.label})!")
            actor.sendMessage("§cThis can cause issues. Backup your data first!")
            actor.sendMessage("§cType the command again to confirm!")
            switchConfirms[confirmKey] = System.currentTimeMillis()
            return
        }

        // Confirm downgrade
        if (current != null && release.version < current &&
            switchConfirms.getOrDefault(confirmKey, 0) + SWITCH_CONFIRM_TIMEOUT_MS < System.currentTimeMillis()
        ) {
            actor.sendMessage("§cYou are downgrading to ${release.version.display}!")
            actor.sendMessage("§cThis can cause issues and data loss!")
            actor.sendMessage("§cType the command again to confirm!")
            switchConfirms[confirmKey] = System.currentTimeMillis()
            return
        }

        switchConfirms.remove(confirmKey)
        Updater.switchVersion(release)
        actor.sendMessage("Activated version: %hc%${release.version.display}")
        actor.sendMessage("§cRestart the node service to apply the changes!")
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
            actor.sendMessage("Download with: ${toConsoleValue("version download <channel> [version]")}")
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
