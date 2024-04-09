package dev.redicloud.updater.suggest

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.updater.Updater
import kotlinx.coroutines.runBlocking

class BuildsSuggester : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = runBlocking {
        val branch = context.input.split(" ").lastOrNull { it.isNotBlank() } ?: return@runBlocking arrayOf("latest")
        return@runBlocking Updater.getBuilds(branch).map { it.build.toString() }.also {
            it.toMutableList().add("latest")
        }.toTypedArray()
    }
}