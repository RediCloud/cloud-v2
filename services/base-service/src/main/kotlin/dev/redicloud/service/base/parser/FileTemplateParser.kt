package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.repository.template.file.FileTemplate

class FileTemplateParser(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : ICommandArgumentParser<FileTemplate> {

    override suspend fun parse(parameter: String): FileTemplate? =
        fileTemplateRepository.getTemplate(parameter)
}
