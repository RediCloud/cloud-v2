package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import kotlinx.coroutines.runBlocking

class CloudServerVersionTypeSuggester(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { serverVersionTypeRepository.getTypes().filter { !it.isUnknown() }.map { it.name }.toTypedArray() }

}