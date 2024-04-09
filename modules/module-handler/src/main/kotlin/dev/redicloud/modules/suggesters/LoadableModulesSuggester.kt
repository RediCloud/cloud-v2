package dev.redicloud.modules.suggesters

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.utils.MODULES_FOLDER
import dev.redicloud.modules.ModuleHandler

class LoadableModulesSuggester(
    private val moduleHandler: ModuleHandler
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        val descriptions = moduleHandler.getCachedDescriptions()
            .filter {
                val data = moduleHandler.getModuleData(it.id)
                data == null || data != null && !data.loaded
            }
        val files = MODULES_FOLDER.getFile().listFiles()
            ?.filter { it.exists() }
            ?.filter { it.isFile }
            ?.filter { it.extension == "jar" }
            ?.filter { file -> moduleHandler.getCachedDescriptions().none { it.cachedFile == file} }
            ?.filter { file ->
                val description = kotlin.runCatching {
                    moduleHandler.loadDescription(file)
                }
                if (description.isFailure) return@filter true
                moduleHandler.getCachedDescriptions().none { it.id == description.getOrNull()!!.id }
            }
            ?: emptyList()
        val results = descriptions.map { it.id } + files.map { it.name }
        return results.toTypedArray()
    }

}