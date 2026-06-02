package dev.redicloud.updater.suggest

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.updater.Updater

/** Suggests available release channels (beta, stable). */
class ChannelSuggester : AbstractCommandSuggester() {
    override suspend fun suggest(context: CommandContext): Array<String> =
        Updater.getAvailableChannels().map { it.label }.toTypedArray()
}
