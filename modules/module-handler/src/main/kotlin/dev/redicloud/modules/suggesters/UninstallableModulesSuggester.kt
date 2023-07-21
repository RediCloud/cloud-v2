package dev.redicloud.modules.suggesters

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.modules.ModuleHandler

class UninstallableModulesSuggester(
    private val moduleHandler: ModuleHandler
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        return moduleHandler.getCachedDescriptions()
            .filter { moduleHandler.getModuleData(it.id)?.loaded == true }
            .map { it.id }
            .toTypedArray()
    }

}