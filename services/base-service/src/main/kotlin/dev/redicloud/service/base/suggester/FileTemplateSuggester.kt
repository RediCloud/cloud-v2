package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class FileTemplateSuggester(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : ICommandSuggester {
    private val easyCache = SingleCache(5.seconds) {
runBlocking { fileTemplateRepository.getTemplates().map { it.getDisplayName() }.toTypedArray() }
    }
    override fun suggest(context: CommandContext): Array<String> = easyCache.get()!!
}