package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository

class ConfigurationTemplateParser(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : ICommandArgumentParser<ConfigurationTemplate> {

    override suspend fun parse(parameter: String): ConfigurationTemplate? =
        configurationTemplateRepository.getTemplate(parameter)
}
