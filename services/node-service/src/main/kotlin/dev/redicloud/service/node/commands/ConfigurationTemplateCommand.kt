package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.suggester.CloudServerVersionSuggester
import dev.redicloud.service.base.suggester.ConfigurationTemplateSuggester
import dev.redicloud.service.base.suggester.FileTemplateSuggester
import dev.redicloud.service.base.suggester.RegisteredCloudNodeSuggester
import dev.redicloud.utils.fileName
import dev.redicloud.utils.isValidUrl
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.runBlocking
import java.net.URL
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Command("configurationtemplate")
@CommandAlias(["ct", "configurationtemplates", "ctemplate"])
@CommandDescription("Manage the configuration templates")
class ConfigurationTemplateCommand(
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val javaVersionRepository: JavaVersionRepository,
    private val serverRepository: ServerRepository,
    private val serverVersionRepository: CloudServerVersionRepository,
    private val nodeRepository: NodeRepository,
    private val fileTemplateRepository: AbstractFileTemplateRepository
) : ICommand {

    companion object {
        val invalidChars = Regex("[/\\\\?%*:|\"<>]")
        val invalidNames = listOf("con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6",
            "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9")
    }

    @CommandSubPath("create <name>")
    @CommandDescription("Create a new configuration template")
    fun create(
        actor: ConsoleActor,
        @CommandParameter("name") name: String
    ) = runBlocking {
        if (configurationTemplateRepository.existsTemplate(name)) {
            actor.sendMessage("§cA configuration template with this name already exists!")
            return@runBlocking
        }
        configurationTemplateRepository.createTemplate(
            ConfigurationTemplate(
                UUID.randomUUID(),
                name,
                512,
                mutableListOf(),
                mutableListOf(),
                0,
                -1,
                0,
                -1,
                100.0,
                "-",
                false,
                50,
                null,
                false,
                40000
            )
        )
        actor.sendMessage("§aThe configuration template was created successfully!")
    }

    @CommandSubPath("delete <name>")
    @CommandDescription("Delete a configuration template")
    fun delete(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate
    ) = runBlocking {
        if (serverRepository.getRegisteredServers().any { it.configurationTemplate.uniqueId == template.uniqueId }) {
            actor.sendMessage("§cThere are still servers registered with this configuration template!")
            actor.sendMessage("§cStop the servers or delete them if it is a static server!")
            return@runBlocking
        }
        configurationTemplateRepository.deleteTemplate(template)
        actor.sendMessage("§aThe configuration template was deleted successfully!")
    }

    @CommandSubPath("list")
    @CommandDescription("List all configuration templates")
    fun list(actor: ConsoleActor) = runBlocking {
        val templates = configurationTemplateRepository.getTemplates()
        if (templates.isEmpty()) {
            actor.sendMessage("§cThere are no configuration templates!")
            return@runBlocking
        }
        actor.sendHeader("Configuration templates")
        actor.sendMessage("")
        templates.forEach {
            actor.sendMessage("§8- §7${it.name}")
        }
        actor.sendMessage("")
        actor.sendHeader("Configuration templates")
    }

    @CommandSubPath("info <name>")
    @CommandDescription("Get information about a configuration template")
    fun info(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate
    ) = runBlocking {
        actor.sendHeader("Configuration template")
        actor.sendMessage("")
        actor.sendMessage("§8- %tc%Name§8: %hc%${template.name}")
        actor.sendMessage("§8- %tc%ID§8: %hc%${template.uniqueId}")
        actor.sendMessage("§8- %tc%Max memory§8: %hc%${template.maxMemory} MB")
        val serverVersion = if (template.serverVersionId != null) {
            serverVersionRepository.getVersion(template.serverVersionId!!)
        }else null
        actor.sendMessage("§8- %tc%Server version§8: %hc%${serverVersion?.displayName ?: "§cNot set"}")
        actor.sendMessage("§8- %tc%Static§8: %hc%${template.static.toSymbol()}")
        actor.sendMessage("§8- %tc%Start priority§8: %hc%${template.startPriority}")
        actor.sendMessage("§8- %tc%Fallback server§8: %hc%${template.fallbackServer.toSymbol()}")
        actor.sendMessage("§8- %tc%Server splitter§8: %hc%${template.serverSplitter}")
        actor.sendMessage("§8- %tc%Start port§8: %hc%${if (template.startPort == -1) "unknown" else template.startPort}")
        actor.sendMessage("§8- %tc%Permission§8: %hc%${template.joinPermission ?: "Not set"}")
        actor.sendMessage("§8- %tc%Max-Players§8: %hc%${template.maxPlayers}")
        actor.sendMessage("§8- %tc%Stop useless servers§8: %hc%${template.timeAfterStopUselessServer.milliseconds.inWholeMinutes} minutes")
        actor.sendMessage("§8- %tc%Min. started servers§8: %hc%${template.minStartedServices}")
        actor.sendMessage("§8- %tc%Max. started servers§8: %hc%${template.maxStartedServices}")
        actor.sendMessage("§8- %tc%Min. started servers per node§8: %hc%${template.minStartedServicesPerNode}")
        actor.sendMessage("§8- %tc%Max. started servers per node§8: %hc%${template.maxStartedServicesPerNode}")
        val nodes = template.nodeIds.mapNotNull { nodeRepository.getNode(it) }
        actor.sendMessage("§8- %tc%Nodes§8: %hc%${if (nodes.isEmpty()) "all nodes" else nodes.joinToString("§8, %hc%") { it.name }}")
        val templates = fileTemplateRepository.collectTemplates(*template.fileTemplateIds
            .mapNotNull { fileTemplateRepository.getTemplate(it) }.toTypedArray()
        ).toMutableList()
        actor.sendMessage("§8- %tc%File templates§8: %hc%${if (templates.isEmpty()) "None" else templates.joinToString("§8, %hc%") { it.displayName }}")
        actor.sendMessage("§8- %tc%JVM arguments§8:${if (template.jvmArguments.isEmpty()) " %hc%None" else ""}")
        template.jvmArguments.forEach {
            actor.sendMessage("§8  - %hc%$it")
        }
        actor.sendMessage("§8- %tc%Program parameter§8:${if (template.programParameters.isEmpty()) " %hc%None" else ""}")
        template.programParameters.forEach {
            actor.sendMessage("§8  - %hc%$it")
        }
        actor.sendMessage("§8- %tc%Environment variables§8:${if (template.environmentVariables.isEmpty()) " %hc%None" else ""}")
        template.environmentVariables.forEach {
            actor.sendMessage("§8  - %hc%${it.key}§8=%tc%${it.value}")
        }
        actor.sendMessage("§8- %tc%Default files§8:${if (template.defaultFiles.isEmpty()) " %hc%None" else ""}")
        template.defaultFiles.forEach {
            actor.sendMessage("§8  - %hc%${it.key} §8➜ %tc%${it.value}")
        }
        actor.sendMessage("§8- %tc%File edits§8:${if (template.fileEdits.isEmpty()) " %hc%None" else ""}")
        template.fileEdits.keys.forEach {
            actor.sendMessage("\t§8- %hc%$it")
            template.fileEdits[it]?.forEach {
                actor.sendMessage("\t    §8- %tc%${it.key} §8➜ %tc%${it.value}")
            }
        }
        actor.sendMessage("")
        actor.sendHeader("Configuration template")
    }

    @CommandSubPath("edit <name> stoptime <minutes>")
    @CommandDescription("Set the time after a server should be stopped if it is useless")
    fun editStopTime(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("time") time: Long
    ) = runBlocking {
        template.timeAfterStopUselessServer = time.minutes.inWholeMilliseconds
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Successfully set the time after a server should be stopped if it is useless was set to ${toConsoleValue(time)} minutes!")
    }

    @CommandSubPath("edit <name> maxplayers <count>")
    @CommandDescription("Set the max players of a configuration template")
    fun editMaxPlayers(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", true, IntegerSuggester::class, ["10", "100", "10"]) count: Int
    ) = runBlocking {
        template.maxPlayers = count
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Successfully set the max players of the configuration template to ${toConsoleValue(count)}!")
    }

    @CommandSubPath("edit <name> fileedits add <file> <key> <value>")
    @CommandDescription("Add a file edit to a configuration template")
    fun editFileEditsAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String,
        @CommandParameter("value") value: String
    ) = runBlocking {
        val subMap = template.fileEdits.getOrDefault(file, mutableMapOf())
        subMap[key] = value
        template.fileEdits[file] = subMap
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Successfully added file edit to the configuration template!")
    }

    @CommandSubPath("edit <name> fileedits remove <file> <key>")
    @CommandDescription("Remove a file edit from a configuration template")
    fun editFileEditsRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String
    ) = runBlocking {
        val subMap = template.fileEdits.getOrDefault(file, mutableMapOf())
        if (subMap.remove(key) == null) {
            actor.sendMessage("§cThe file edit with the key ${toConsoleValue(key, false)} does not exist!")
            return@runBlocking
        }
        if (subMap.isEmpty()) {
            template.fileEdits.remove(file)
        } else {
            template.fileEdits[file] = subMap
        }
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Successfully removed file edit from configuration template!")
    }

    @CommandSubPath("edit <name> files add <url> [path]")
    @CommandDescription("Add a default file to a configuration template")
    fun editFilesAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("url") url: String,
        @CommandParameter("path", true) path: String?
    ) = runBlocking {
        if (!isValidUrl(url)) {
            actor.sendMessage("§cThe url '$url' is not valid!")
            return@runBlocking
        }
        val file = path ?: URL(url).fileName
        if (template.defaultFiles.any { it.value.lowercase() == file.lowercase() }) {
            actor.sendMessage("§cThe file with the url ${toConsoleValue(url, false)} was already added to the configuration template ${toConsoleValue(template.name, false)}!")
            return@runBlocking
        }
        template.defaultFiles[path ?: url] = url
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Added file with url ${toConsoleValue(url)} to the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> files remove <url>")
    @CommandDescription("Remove a default file from a configuration template")
    fun editFilesRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("url") url: String
    ) = runBlocking {
        if (template.defaultFiles.none { it.value.lowercase() == url.lowercase() }) {
            actor.sendMessage("§cThe file with the url ${toConsoleValue(url)} is not added to the configuration template ${toConsoleValue(template.name, false)}!")
            return@runBlocking
        }
        template.defaultFiles.remove(url)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Removed file with url ${toConsoleValue(url)} from the configuration template ${toConsoleValue(template.name)}!")
    }



    @CommandSubPath("edit <name> name <new-name>")
    @CommandDescription("Edit the name of a configuration template")
    fun editName(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("new-name") newName: String
    ) = runBlocking {
        if (configurationTemplateRepository.existsTemplate(newName)) {
            actor.sendMessage("§cA configuration template with this name already exists!")
            return@runBlocking
        }
        template.name = newName
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The name was changed to ${toConsoleValue(newName)}!")
    }

    @CommandSubPath("edit <name> programparameter add <parameter>")
    @CommandDescription("Add a program parameter to a configuration template")
    fun editProgramParameterAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("parameter") parameter: String
    ) = runBlocking {
        template.programParameters.add(parameter)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The program parameter ${toConsoleValue(parameter)} was added to the configuration template ${toConsoleValue(template.name)}!")
    }



    @CommandSubPath("edit <name> programparameter remove <parameter>")
    @CommandDescription("Remove a program parameter from a configuration template")
    fun editProgramParameterRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("parameter") parameter: String
    ) = runBlocking {
        template.programParameters.remove(parameter)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The program parameter ${toConsoleValue(parameter)} was removed from the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> jvmargument add <argument>")
    @CommandAlias(["edit <name> ja <argument> add <argument>"])
    @CommandDescription("Add a JVM argument to a configuration template")
    fun editJvmArgumentAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("argument") argument: String
    ) = runBlocking {
        template.jvmArguments.add(argument)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The JVM argument ${toConsoleValue(argument)} was added to the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> jvmargument remove <argument>")
    @CommandAlias(["edit <name> ja <argument> remove <argument>"])
    @CommandDescription("Remove a JVM argument from a configuration template")
    fun editJvmArgumentRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("argument") argument: String
    ) = runBlocking {
        template.jvmArguments.remove(argument)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The JVM argument ${toConsoleValue(argument)} was removed from the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> environment add <key> <value>")
    @CommandAlias(["edit <name> env add <key> <value>"])
    @CommandDescription("Add an environment variable to a configuration template")
    fun editEnvironmentAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("key") key: String,
        @CommandParameter("value") value: String
    ) = runBlocking {
        template.environmentVariables[key] = value
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The environment variable ${toConsoleValue(key)} was added to the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> environment remove <key>")
    @CommandAlias(["edit <name> env remove <key>"])
    @CommandDescription("Remove an environment variable from a configuration template")
    fun editEnvironmentRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("key") key: String,
        @CommandParameter("value") value: String
    ) = runBlocking {
        template.environmentVariables.remove(key)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The environment variable ${toConsoleValue(key)} was removed from the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> maxmemory <memory>")
    @CommandAlias(["edit <name> mm <memory>"])
    @CommandDescription("Edit the maximum memory of a configuration template")
    fun editMaxMemory(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("memory", true, MemorySuggester::class) memory: Long
    ) = runBlocking {
        template.maxMemory = memory
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The maximum memory of the configuration template ${toConsoleValue(template.name)} was updated to ${toConsoleValue(memory)}!")
    }

    @CommandSubPath("edit <name> filetemplate add <template>")
    @CommandAlias(["edit <name> ft add <name>"])
    @CommandDescription("Add a file template to a configuration template")
    fun editFileTemplateAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("template", true, FileTemplateSuggester::class) fileTemplate: FileTemplate
    ) = runBlocking {
        template.fileTemplateIds.add(fileTemplate.uniqueId)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(fileTemplate.name)} added to configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> filetemplate remove <template>")
    @CommandAlias(["edit <name> ft remove <name>"])
    @CommandDescription("Remove a file template from a configuration template")
    fun editFileTemplateRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("template", true, FileTemplateSuggester::class) fileTemplate: FileTemplate
    ) = runBlocking {
        template.fileTemplateIds.remove(fileTemplate.uniqueId)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("File template ${toConsoleValue(fileTemplate.name)} removed from configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> node add <node>")
    @CommandAlias(["edit <name> node add <node>"])
    @CommandDescription("Add a node to a configuration template")
    fun editNodeAdd(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("node", true, RegisteredCloudNodeSuggester::class) node: CloudNode
    ) = runBlocking {
        template.nodeIds.add(node.serviceId)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The node ${toConsoleValue(node.name)} will now be used to start servers of the configuration template ${toConsoleValue(template.name)}!")
    }

    @CommandSubPath("edit <name> node remove <node>")
    @CommandAlias(["edit <name> node remove <node>"])
    @CommandDescription("Remove a node from a configuration template")
    fun editNodeRemove(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("node", true, RegisteredCloudNodeSuggester::class) node: CloudNode
    ) = runBlocking {
        template.nodeIds.remove(node.serviceId)
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The node ${toConsoleValue(node.name)} will no longer be used to start servers of the configuration template ${toConsoleValue(template.name)}!")
        val servers = serverRepository.getRegisteredServers().filter { it.hostNodeId == node.serviceId }
        if (servers.isNotEmpty()) {
            actor.sendMessage("There are still servers registered on this node:")
            servers.forEach { server ->
                actor.sendMessage("§8- %hc%${server.name} §7(${server.serviceId.id}) §8| %tc%Connected§8: ${(server.currentSession != null).toSymbol()}")
            }
        }
    }

    @CommandSubPath("edit <name> minServices <count>")
    @CommandDescription("Edit the minimum amount of started services of a configuration template")
    fun editMinStartedServices(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", true, IntegerSuggester::class) count: Int
    ) = runBlocking {
        if (count != -1) {
            if (template.maxStartedServices in 1 until count) {
                actor.sendMessage("The minimum amount of started services cannot be higher than the maximum amount of started services!")
                return@runBlocking
            }
            if (template.maxStartedServicesPerNode in 1 until count) {
                actor.sendMessage("The minimum amount of started services cannot be higher than the maximum amount of started services per node!")
                return@runBlocking
            }
        }
        template.minStartedServices = count
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The minimum amount of started services of the configuration template ${toConsoleValue(template.name)} was updated to ${toConsoleValue(count)}!")
    }

    @CommandSubPath("edit <name> maxServices <count>")
    @CommandDescription("Edit the maximum amount of started services of a configuration template")
    fun editMaxStartedServices(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", true, IntegerSuggester::class) count: Int
    ) = runBlocking {
        if (count != -1) {
            if (count < template.minStartedServices && template.minStartedServices > 0) {
                actor.sendMessage("The maximum amount of started services cannot be lower than the minimum amount of started services!")
                return@runBlocking
            }
            if (count < template.minStartedServicesPerNode && template.minStartedServicesPerNode > 0) {
                actor.sendMessage("The maximum amount of started services cannot be lower than the minimum amount of started services per node!")
                return@runBlocking
            }
        }
        template.maxStartedServices = count
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The maximum amount of started services of the configuration template ${toConsoleValue(template.name)} was updated to ${toConsoleValue(count)}!")
    }

    @CommandSubPath("edit <name> minServicesPerNode <count>")
    @CommandDescription("Edit the minimum amount of started services per node of a configuration template")
    fun editMinStartedServicesPerNode(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", true, IntegerSuggester::class) count: Int
    ) = runBlocking {
        if (count != -1) {
            if (template.minStartedServices in 1 until count) {
                actor.sendMessage("The minimum amount of started services per node cannot be higher than the maximum amount of started services!")
                return@runBlocking
            }
            if (template.maxStartedServicesPerNode in 1 until count) {
                actor.sendMessage("The minimum amount of started services per node cannot be higher than the maximum amount of started services per node!")
                return@runBlocking
            }
            if (template.maxStartedServices in 1 until count) {
                actor.sendMessage("The minimum amount of started services per node cannot be higher than the maximum amount of started services!")
                return@runBlocking
            }
        }
        template.minStartedServicesPerNode = count
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The minimum amount of started services per node of the configuration template ${toConsoleValue(template.name)} was updated to ${toConsoleValue(count)}!")
    }

    @CommandSubPath("edit <name> maxServicesPerNode <count>")
    @CommandDescription("Edit the maximum amount of started services per node of a configuration template")
    fun editMaxStartedServicesPerNode(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", true, IntegerSuggester::class) count: Int
    ) = runBlocking {
        if (count != -1) {
            if (count < template.minStartedServicesPerNode && template.minStartedServicesPerNode > 0) {
                actor.sendMessage("The maximum amount of started services per node cannot be lower than the minimum amount of started services per node!")
                return@runBlocking
            }
            if (template.maxStartedServices in 1 until count) {
                actor.sendMessage("The maximum amount of started services per node cannot be higher than the maximum amount of started services!")
                return@runBlocking
            }
            if (template.maxStartedServicesPerNode in 1 until count) {
                actor.sendMessage("The maximum amount of started services per node cannot be higher than the maximum amount of started services per node!")
                return@runBlocking
            }
        }
        template.maxStartedServicesPerNode = count
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The maximum amount of started services per node of the configuration template ${toConsoleValue(template.name)} was updated to ${toConsoleValue(count)}!")
    }

    @CommandSubPath("edit <name> percentToStartNew <percent>")
    @CommandDescription("Edit the percent of player online to start a new service of a configuration template")
    fun editPercentToStartNew(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("percent", true) percent: Double
    ) = runBlocking {
        template.percentToStartNewService = percent
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("If the percent of players, on a server of ${toConsoleValue(template.name)}, is higher than ${toConsoleValue(percent)}% a new service will be started!")
    }

    @CommandSubPath("edit <name> splitter <value>")
    @CommandDescription("Edit the server splitter of a configuration template")
    fun editSplitter(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("value", true) splitter: String
    ) = runBlocking {
        if (splitter.length > 3) {
            actor.sendMessage("§cThe splitter cannot be longer than 3 characters!")
            return@runBlocking
        }
        if (invalidChars.containsMatchIn(splitter)) {
            actor.sendMessage("§cThe splitter cannot contain any of the following characters: ${invalidChars.pattern.split("").joinToString(", ")}")
            return@runBlocking
        }
        if (invalidNames.contains(splitter.lowercase())) {
            actor.sendMessage("§cThe splitter cannot be any of the following names: ${invalidNames.joinToString(", ")}")
            return@runBlocking
        }
        val servers = serverRepository.getRegisteredServers().filter { it.configurationTemplate.uniqueId == template.uniqueId }
        if (servers.isNotEmpty()) {
            actor.sendMessage("§cThe splitter cannot be changed while there are still servers registered on this template!")
            actor.sendMessage("§cRegistered servers: ${servers.joinToString(", ") { it.name }}")
            return@runBlocking
        }
        template.serverSplitter = if (splitter == "''") "" else splitter
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The splitter of the template ${toConsoleValue(template.name)} was updated to ${toConsoleValue(if (splitter == "''") "" else splitter)}!")
    }

    @CommandSubPath("edit <name> fallback <value>")
    @CommandDescription("Edit if the fallback server should be enabled or not of a configuration template")
    fun editFallback(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("value", true, BooleanSuggester::class) fallback: Boolean
    ) = runBlocking {
        template.fallbackServer = fallback
        configurationTemplateRepository.updateTemplate(template)
        if (fallback) {
            actor.sendMessage("The servers of the template ${toConsoleValue(template.name)} will now be fallback servers!")
        } else {
            actor.sendMessage("The servers of the template ${toConsoleValue(template.name)} will no longer be fallback servers!")
        }
    }

    @CommandSubPath("edit <name> startPriority <priority>")
    @CommandDescription("Edit the start priority of a configuration template")
    fun editStartPriority(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("priority", true, IntegerSuggester::class, ["0", "100"]) priority: Int
    ) = runBlocking {
        if (priority < 0 || priority > 100) {
            actor.sendMessage("§cThe priority must be between 0 and 100!")
            return@runBlocking
        }
        template.startPriority = priority
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The start priority of ${toConsoleValue(template.name)} was updated to ${toConsoleValue(priority)}!")
    }

    @CommandSubPath("edit <name> version <version>")
    @CommandDescription("Edit the server version of a configuration template")
    fun editVersion(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) = runBlocking {
        template.serverVersionId = version.uniqueId
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The version of ${toConsoleValue(template.name)} was updated to ${toConsoleValue(version.displayName)}!")
    }

    @CommandSubPath("edit <name> startport <port>")
    @CommandDescription("Edit the start port!")
    fun editStartPort(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("port", true) port: Int
    ) = runBlocking {
        if (port < 100 || port > 65535) {
            actor.sendMessage("§cThe port must be between 100 and 65535!")
            return@runBlocking
        }
        template.startPort = port
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("The start port of ${toConsoleValue(template.name)} was updated to ${toConsoleValue(port)}!")
    }


    @CommandSubPath("edit <name> static <state>")
    @CommandAlias(["edit <name> static <state>"])
    @CommandDescription("Edit the static service state of a configuration template")
    fun editStaticService(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("state", true, BooleanSuggester::class) staticService: Boolean
    ) = runBlocking {
        val servers = serverRepository.getRegisteredServers().filter { it.configurationTemplate.uniqueId == template.uniqueId }
        if (servers.isNotEmpty()) {
            actor.sendMessage("§cYou cannot edit the static state of a template while there are still servers registered! Delete all static servers or stop all dynamic servers first!")
            actor.sendMessage("§cRegistered servers: ${servers.joinToString(", ") { it.name}}")
        }
        template.static = staticService
        configurationTemplateRepository.updateTemplate(template)
        actor.sendMessage("Static service of ${toConsoleValue(template.name)} was updated to ${toConsoleValue(staticService)}!")
    }

    @CommandSubPath("edit <name> permission <permission>")
    @CommandDescription("Edit the permission that is required to join a service of a configuration template! Use 'null' to remove the permission")
    fun editPermission(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("permission", true) permission: String
    ) = runBlocking {
        template.joinPermission = if (permission == "null") null else permission
        configurationTemplateRepository.updateTemplate(template)
        if (permission == "null") {
            actor.sendMessage("The permission of ${toConsoleValue(template.name)} was removed successfully!")
        }else {
            actor.sendMessage("The permission of ${toConsoleValue(template.name)} was updated to '${toConsoleValue(permission)}'!")
        }
    }

}