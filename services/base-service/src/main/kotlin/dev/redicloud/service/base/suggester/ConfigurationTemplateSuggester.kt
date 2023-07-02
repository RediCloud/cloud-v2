package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class ConfigurationTemplateSuggester(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : ICommandSuggester {

    private val easyCache = SingleCache(5.seconds) { configurationTemplateRepository.getTemplates().map { it.name }.toTypedArray() }
    override fun suggest(context: CommandContext): Array<String> = easyCache.get()!!
}