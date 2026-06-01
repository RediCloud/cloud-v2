package dev.redicloud.updater.suggest

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.updater.Updater

class BranchSuggester : AbstractCommandSuggester() {
    override suspend fun suggest(context: CommandContext): Array<String> =
        Updater.getBranches().toTypedArray()
}
