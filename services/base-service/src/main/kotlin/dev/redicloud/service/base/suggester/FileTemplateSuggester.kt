package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository

class FileTemplateSuggester(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        fileTemplateRepository.getTemplates().map { it.displayName }.toTypedArray()
}
