package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository

class CloudServerVersionTypeSuggester(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        serverVersionTypeRepository.getTypes().filter { !it.isUnknown() }.map { it.name }.toTypedArray()
}
