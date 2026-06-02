package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository

class ConfigurationTemplateSuggester(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        configurationTemplateRepository.getTemplates().map { it.name }.toTypedArray()
}
