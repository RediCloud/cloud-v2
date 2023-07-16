package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import kotlinx.coroutines.runBlocking

class FileTemplateParser(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : ICommandArgumentParser<FileTemplate> {

    override fun parse(parameter: String): FileTemplate? =
        runBlocking { fileTemplateRepository.getTemplate(parameter) }
}