package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import kotlinx.coroutines.runBlocking

class ConfigurationTemplateParser(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : ICommandArgumentParser<ConfigurationTemplate> {

    override fun parse(parameter: String): ConfigurationTemplate? =
        runBlocking { configurationTemplateRepository.getTemplate(parameter) }

}