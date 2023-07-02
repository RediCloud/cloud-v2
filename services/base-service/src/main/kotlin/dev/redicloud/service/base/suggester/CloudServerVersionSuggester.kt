package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.utils.EasyCache
import dev.redicloud.utils.SingleCache
import kotlin.time.Duration.Companion.seconds

class CloudServerVersionSuggester(
    private val cloudServerVersionRepository: CloudServerVersionRepository
) : ICommandSuggester {

    private val easyCache = SingleCache(5.seconds) { cloudServerVersionRepository.getAll().map { it.getDisplayName() }.toTypedArray() }
    override fun suggest(context: CommandContext): Array<String> =
        easyCache.get()!!
}