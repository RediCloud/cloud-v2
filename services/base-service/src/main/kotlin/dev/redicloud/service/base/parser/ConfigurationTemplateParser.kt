package dev.redicloud.service.base.parser

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import kotlinx.coroutines.runBlocking

class ConfigurationTemplateParser(
    private val configurationTemplateRepository: ConfigurationTemplateRepository
) : CommandArgumentParser<ConfigurationTemplate> {

    override fun parse(parameter: String): ConfigurationTemplate? =
        runBlocking { configurationTemplateRepository.getTemplate(parameter) }

}