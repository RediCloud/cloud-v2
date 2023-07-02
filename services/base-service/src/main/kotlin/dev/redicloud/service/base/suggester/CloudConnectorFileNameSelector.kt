package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.utils.CONNECTORS_FOLDER

class CloudConnectorFileNameSelector : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> {
        return CONNECTORS_FOLDER.getFile().listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf()
    }

}