package dev.redicloud.updater.suggest

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.updater.Updater

class BuildsSuggester : AbstractCommandSuggester() {
    override suspend fun suggest(context: CommandContext): Array<String> {
        val branch = context.input.split(" ").lastOrNull { it.isNotBlank() } ?: return arrayOf("latest")
        return Updater.getBuilds(branch).map { it.build.toString() }.also {
            it.toMutableList().add("latest")
        }.toTypedArray()
    }
}
