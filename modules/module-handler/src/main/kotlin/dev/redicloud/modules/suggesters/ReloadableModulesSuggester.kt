package dev.redicloud.modules.suggesters

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.modules.ModuleHandler

class ReloadableModulesSuggester(
    private val moduleHandler: ModuleHandler
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        return moduleHandler.getModuleDatas()
            .filter { moduleHandler.isModuleReloadable(it.id) }
            .map { it.id }
            .toTypedArray()
    }

}