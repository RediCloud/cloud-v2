package dev.redicloud.updater.suggest

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.updater.Updater
import dev.redicloud.utils.version.VersionChannel

/** Suggests available version strings for a given channel. */
class VersionSuggester : AbstractCommandSuggester() {
    override suspend fun suggest(context: CommandContext): Array<String> {
        val channelStr = context.input.split(" ").lastOrNull { it.isNotBlank() }
        val channel = channelStr?.let { VersionChannel.fromLabelOrNull(it) }
            ?: VersionChannel.STABLE

        return Updater.getReleasesByChannel(channel)
            .map { it.version.display }
            .plus("latest")
            .toTypedArray()
    }
}
