package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.api.version.IServerVersionHandler

class ServerVersionHandlerParser : ICommandArgumentParser<IServerVersionHandler> {

    override suspend fun parse(parameter: String): IServerVersionHandler? {
        return IServerVersionHandler.CACHE_HANDLERS.find { it.name.equals(parameter, ignoreCase = true) }
    }
}
