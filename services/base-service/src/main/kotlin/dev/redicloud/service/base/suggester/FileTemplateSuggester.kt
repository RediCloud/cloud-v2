package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import kotlinx.coroutines.runBlocking

class FileTemplateSuggester(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { fileTemplateRepository.getTemplates().map { it.getDisplayName() }.toTypedArray() }
}