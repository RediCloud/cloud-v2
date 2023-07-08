package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import kotlinx.coroutines.runBlocking

class FileTemplateSuggester(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { fileTemplateRepository.getTemplates().map { it.getDisplayName() }.toTypedArray() }
}