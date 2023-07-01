package dev.redicloud.service.base.parser

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import kotlinx.coroutines.runBlocking

class FileTemplateParser(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : CommandArgumentParser<FileTemplate> {

    override fun parse(parameter: String): FileTemplate? =
        runBlocking { fileTemplateRepository.getTemplate(parameter) }
}