package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import kotlinx.coroutines.runBlocking

class ConfigurationTemplateSuggester(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { configurationTemplateRepository.getTemplates().map { it.name }.toTypedArray() }
}