package dev.redicloud.modules.suggesters

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.utils.MODULE_FOLDER
import dev.redicloud.modules.ModuleHandler

class LoadablesModulesSuggester(
    private val moduleHandler: ModuleHandler
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        val descriptions = moduleHandler.getCachedDescriptions()
            .filter { moduleHandler.getModuleData(it.id)?.loaded == true }
        val files = MODULE_FOLDER.getFile().listFiles()
            ?.filter { it.exists() }
            ?.filter { it.isFile }
            ?.filter { it.extension == "jar" }
            ?.filter { file -> moduleHandler.getCachedDescriptions().any { it.cachedFile == file} }
            ?: emptyList()
        val results = descriptions.map { it.id } + files.map { it.name }
        return results.toTypedArray()
    }

}