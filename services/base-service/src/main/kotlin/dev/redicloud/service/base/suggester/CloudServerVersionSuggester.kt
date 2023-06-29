package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

class CloudServerVersionSuggester(
    private val serverVersionRepository: ServerVersionRepository
) : ICommandSuggester {

    private val easyCache = EasyCache<List<CloudServerVersion>, Unit>(5.seconds) { serverVersionRepository.getAll() }

    override fun suggest(context: CommandContext): Array<String> =
        easyCache.get()?.map { it.name }?.toTypedArray() ?: emptyArray()
}