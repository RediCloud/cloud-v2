package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.template.file.FileTemplateRepository
import kotlinx.coroutines.runBlocking

class FileTemplateSuggester(
    private val fileTemplateRepository: FileTemplateRepository
) : ICommandSuggester {
    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { fileTemplateRepository.getTemplates().map { it.getDisplayName() }.toTypedArray() }
}