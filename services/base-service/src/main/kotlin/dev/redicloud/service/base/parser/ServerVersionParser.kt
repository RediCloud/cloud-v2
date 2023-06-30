package dev.redicloud.service.base.parser

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.repository.server.version.utils.ServerVersion

class ServerVersionParser : CommandArgumentParser<ServerVersion> {
    override fun parse(parameter: String): ServerVersion? =
        ServerVersion.versions().firstOrNull { it.name.lowercase() == parameter.lowercase() }


}