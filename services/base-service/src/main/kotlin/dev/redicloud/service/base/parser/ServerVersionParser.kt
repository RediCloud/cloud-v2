package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.server.version.serverversion.ServerVersion
import dev.redicloud.repository.server.version.serverversion.VersionRepository

class ServerVersionParser : ICommandArgumentParser<ServerVersion> {
    override fun parse(parameter: String): ServerVersion? =
        VersionRepository.versions().firstOrNull { it.name.lowercase() == parameter.lowercase() }


}