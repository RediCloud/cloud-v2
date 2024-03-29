package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.suggester.ConnectedCloudNodeSuggester
import dev.redicloud.service.base.suggester.FileTemplateSuggester
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

@Command("filetemplate")
@CommandAlias(["ft", "filetemplates", "ftemplate"])
@CommandDescription("Manage the file templates")
class FileTemplateCommand(
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : ICommand {

    @CommandSubPath("duplicate <name> [new-name]")
    @CommandDescription("Duplicate a file template")
    fun duplicate(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate,
        @CommandParameter("new-name", false) newName: String?
    ) = defaultScope.launch {
        val newTemplate = template.copy(newName ?: "${template.name}_copy")
        if (fileTemplateRepository.existsTemplate(newTemplate.name, newTemplate.prefix)) {
            actor.sendMessage("§cA file template with the name ${toConsoleValue(newTemplate.name)} and prefix ${toConsoleValue(newTemplate.prefix)} already exists!")
            return@launch
        }
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will be duplicated to ${toConsoleValue(newTemplate.displayName)}...")
        if (!newTemplate.folder.exists() && template.folder.exists()) {
            template.folder.copyRecursively(newTemplate.folder)
        }
        fileTemplateRepository.createTemplate(newTemplate)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} was duplicated to ${toConsoleValue(newTemplate.displayName)}!")
    }

    @CommandSubPath("list")
    @CommandDescription("List all file templates")
    fun list(
        actor: ConsoleActor
    ) = runBlocking {
        val v = fileTemplateRepository.getTemplates()
        val versions = mutableMapOf<String, MutableList<String>>()
        v.forEach {
            val list = versions.getOrDefault(it.prefix, mutableListOf())
            list.add(it.name)
            versions[it.prefix] = list
        }
        if (versions.isEmpty()) {
            actor.sendMessage("§cNo file templates found")
            return@runBlocking
        }
        actor.sendHeader("File templates")
        actor.sendMessage("")
        versions.forEach { (prefix, values) ->
            actor.sendMessage("§8- %hc%$prefix §8(%tc%${values.size}§8)")
            values.forEach {
                actor.sendMessage("  §8➥ %tc%$it")
            }
        }
        actor.sendMessage("")
        actor.sendHeader("File templates")
    }

    @CommandSubPath("info <name>")
    @CommandDescription("Get info about a file template")
    fun info(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate
    ) = runBlocking {
        val inherited = fileTemplateRepository.collectTemplates(template)
        actor.sendHeader("File template")
        actor.sendMessage("")
        actor.sendMessage("§8- %tc%Name§8: %hc%${template.name}")
        actor.sendMessage("§8- %tc%Prefix§8: %hc%${template.prefix}")
        actor.sendMessage("§8- %tc%Inherited§8:${if(inherited.isEmpty()) " %hc%None" else ""}")
        inherited.forEach {
            actor.sendMessage("  §8➥ %tc%${it.displayName}")
        }
        actor.sendMessage("")
        actor.sendHeader("File template")
    }

    @CommandSubPath("create <name> <prefix>")
    @CommandDescription("Create a new file template")
    fun create(
        actor: ConsoleActor,
        @CommandParameter("name") name: String,
        @CommandParameter("prefix") prefix: String
    ) = defaultScope.launch {
        if (fileTemplateRepository.existsTemplate(name, prefix)) {
            actor.sendMessage("§cA file template with the name ${toConsoleValue(name)} and prefix ${toConsoleValue(prefix)} already exists!")
            return@launch
        }
        val template = FileTemplate(
            UUID.randomUUID(),
            prefix,
            name,
            mutableListOf()
        )
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will be created...")
        fileTemplateRepository.createTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} was created!")
    }

    @CommandSubPath("delete <name>")
    @CommandDescription("Delete a file template")
    fun delete(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate
    ) = defaultScope.launch {
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will be deleted...")
        fileTemplateRepository.deleteTemplate(template.uniqueId)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} was deleted!")
    }

    @CommandSubPath("edit <name> inherit add <inherit>")
    @CommandDescription("Add a file template to the inheritance of a file template")
    fun editInheritAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate,
        @CommandParameter("inherit", true, FileTemplateSuggester::class) inherit: FileTemplate
    ) = runBlocking {
        if (template.inherited.contains(inherit.uniqueId)) {
            actor.sendMessage("§cThe file template ${toConsoleValue(template.displayName, false)} already inherits from ${toConsoleValue(inherit.displayName, false)}!")
            return@runBlocking
        }
        val allTemplates = fileTemplateRepository.collectTemplates(template)
        if (allTemplates.contains(inherit)) {
            actor.sendMessage("§cThe file template ${toConsoleValue(template.displayName, false)} already inherits from ${toConsoleValue(inherit.displayName, false)}!")
            return@runBlocking
        }
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will inherit from ${toConsoleValue(inherit.displayName)}...")
        template.inherited.add(template.uniqueId)
        fileTemplateRepository.updateTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} now inherits from ${toConsoleValue(inherit.displayName)}!")
    }

    @CommandSubPath("edit <name> inherit remove <inherit>")
    @CommandDescription("Remove a file template from the inheritance of a file template")
    fun editInheritRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate,
        @CommandParameter("inherit", true, FileTemplateSuggester::class) inherit: FileTemplate
    ) = runBlocking {
        if (!template.inherited.contains(inherit.uniqueId)) {
            actor.sendMessage("§cThe file template ${toConsoleValue(template.displayName, false)} does not inherit from ${toConsoleValue(inherit.displayName, false)}!")
            return@runBlocking
        }
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will no longer inherit from ${toConsoleValue(inherit.displayName)}...")
        template.inherited.remove(template.uniqueId)
        fileTemplateRepository.updateTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} no longer inherits from ${toConsoleValue(inherit.displayName)}!")
    }

    @CommandSubPath("edit <name> name <new-name>")
    @CommandDescription("Change the name of a file template")
    fun editName(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate,
        @CommandParameter("new-name") newName: String
    ) = defaultScope.launch {
        if (fileTemplateRepository.existsTemplate(newName, template.prefix)) {
            actor.sendMessage("§cA file template with the name ${toConsoleValue(newName)} and prefix ${toConsoleValue(template.prefix)} already exists!")
            return@launch
        }
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will be renamed to ${toConsoleValue(newName)}...")
        template.name = newName
        fileTemplateRepository.updateTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} was renamed to ${toConsoleValue(newName)}!")
    }

    @CommandSubPath("edit <name> prefix <new-prefix>")
    @CommandDescription("Change the prefix of a file template")
    fun editPrefix(
        actor: ConsoleActor,
        @CommandParameter("name", true, FileTemplateSuggester::class) template: FileTemplate,
        @CommandParameter("new-prefix") newPrefix: String
    ) = defaultScope.launch {
        if (fileTemplateRepository.existsTemplate(template.name, newPrefix)) {
            actor.sendMessage("§cA file template with the name ${toConsoleValue(template.name)} and prefix ${toConsoleValue(newPrefix)} already exists!")
            return@launch
        }
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} will be renamed to ${toConsoleValue(newPrefix)}...")
        template.prefix = newPrefix
        fileTemplateRepository.updateTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(template.displayName)} was renamed to ${toConsoleValue(newPrefix)}!")
    }

    @CommandSubPath("publish <node>")
    @CommandDescription("Publish a file template to a node")
    fun publish(
        actor: ConsoleActor,
        @CommandParameter("node", true, ConnectedCloudNodeSuggester::class) node: CloudNode,
    ) = defaultScope.launch {
        if (node.currentSession == null) {
            actor.sendMessage("§cThe node ${toConsoleValue(node.name)} is not connected!")
            return@launch
        }
        fileTemplateRepository.pushTemplates(node.serviceId)
    }

}