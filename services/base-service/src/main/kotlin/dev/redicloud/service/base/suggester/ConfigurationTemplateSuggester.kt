package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import kotlinx.coroutines.runBlocking

class ConfigurationTemplateSuggester(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { configurationTemplateRepository.getTemplates().map { it.name }.toTypedArray() }
}