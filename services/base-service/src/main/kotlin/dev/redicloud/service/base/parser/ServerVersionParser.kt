package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.server.version.utils.ServerVersion

class ServerVersionParser : ICommandArgumentParser<ServerVersion> {
    override fun parse(parameter: String): ServerVersion? =
        ServerVersion.versions().firstOrNull { it.name.lowercase() == parameter.lowercase() }


}