package dev.redicloud.modules.suggesters

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.modules.ModuleHandler

class UnloadableModulesSuggester(
    private val moduleHandler: ModuleHandler
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        val modules = moduleHandler.getModuleDatas()
        val loaded = modules.filter { it.lifeCycle == ModuleLifeCycle.LOAD || it.loaded }.map { it.description }
        val results = loaded + moduleHandler.getCachedDescriptions()
            .filter { desc -> loaded.any { it.id == desc.id } }
        return results
            .map { it.id }
            .toTypedArray()
    }

}