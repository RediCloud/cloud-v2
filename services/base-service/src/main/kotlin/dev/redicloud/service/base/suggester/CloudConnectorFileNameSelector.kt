package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.utils.CONNECTORS_FOLDER

class CloudConnectorFileNameSelector : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> {
        return CONNECTORS_FOLDER.getFile().listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf()
    }
}
