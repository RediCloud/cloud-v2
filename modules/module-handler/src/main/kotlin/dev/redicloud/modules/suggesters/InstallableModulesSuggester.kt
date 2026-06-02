package dev.redicloud.modules.suggesters

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.modules.ModuleHandler

class InstallableModulesSuggester(
    private val moduleHandler: ModuleHandler
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> {
        return moduleHandler.repositories.flatMap { it.getModuleIds() }
            .filter { id -> moduleHandler.getCachedDescriptions().none { it.id == id } }
            .toTypedArray()
    }
}
