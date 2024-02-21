package dev.redicloud.updater.suggest

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.updater.Updater
import kotlinx.coroutines.runBlocking

class BranchSuggester : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = runBlocking {
        return@runBlocking Updater.getBranches().toTypedArray()
    }
}