package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.api.repositories.version.IServerVersionHandler

class ServerVersionHandlerParser : ICommandArgumentParser<IServerVersionHandler> {

    override fun parse(parameter: String): IServerVersionHandler? {
        return IServerVersionHandler.CACHE_HANDLERS.find { it.name.lowercase() == parameter.lowercase() }
    }

}