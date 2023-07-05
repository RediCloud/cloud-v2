package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class CloudServerVersionSuggester(
    private val cloudServerVersionRepository: CloudServerVersionRepository
) : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { cloudServerVersionRepository.getVersions().map { it.getDisplayName() }.toTypedArray() }
}