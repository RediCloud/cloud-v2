package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.service.base.suggester.CloudServerVersionTypeSuggester
import dev.redicloud.service.base.suggester.ServerVersionHandlerSuggester
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.runBlocking

@Command("svt")
@CommandAlias(["serverversiontype", "svtype"])
class CloudServerVersionTypeCommand(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val serverVersionRepository: CloudServerVersionRepository
) : CommandBase() {

    @CommandSubPath("list")
    @CommandDescription("List all server version types")
    fun list(
        actor: ConsoleActor
    ) {
        runBlocking {
            val types = serverVersionTypeRepository.getTypes()
            if (types.isEmpty()) {
                actor.sendMessage("No server version types found!")
                return@runBlocking
            }
            actor.sendMessage("Server version types§8:")
            types.forEach {
                actor.sendMessage(
                    "§8- %hc%${it.name}"
                )
            }
        }
    }

    @CommandSubPath("handlers")
    @CommandDescription("List all server version handlers")
    fun handlers(
        actor: ConsoleActor
    ) {
        runBlocking {
            val handlers = IServerVersionHandler.CACHE_HANDLERS
            if (handlers.isEmpty()) {
                actor.sendMessage("No server version handlers found!")
                return@runBlocking
            }
            actor.sendMessage("Server version handlers§8:")
            handlers.forEach {
                actor.sendMessage(
                    "§8- %hc%${it.name}"
                )
            }
        }
    }

    @CommandSubPath("info <name>")
    @CommandDescription("Get info about a server version type")
    fun info(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType
    ) {
        actor.sendMessage("§8<====== %hc%§nServer-Version type§8 ======§8>")
        actor.sendMessage("")
        actor.sendMessage("Name§8: %hc%${type.name}")
        actor.sendMessage("Handler§8: %hc%${type.versionHandlerName}")
        actor.sendMessage("Default§8: %hc%${type.defaultType.toSymbol()}")
        actor.sendMessage("CraftBukkit based§8: %hc%${type.craftBukkitBased.toSymbol()}")
        actor.sendMessage("Proxy§8: %hc%${type.proxy.toSymbol()}")
        actor.sendMessage("JVM arguments§8:${if (type.jvmArguments.isEmpty()) " %hc%None" else ""}")
        type.jvmArguments.forEach {
            actor.sendMessage("\t§8- %hc%$it")
        }
        actor.sendMessage("Programm arguments§8:${if (type.programmArguments.isEmpty()) " %hc%None" else ""}")
        type.programmArguments.forEach {
            actor.sendMessage("\t§8- %hc%$it")
        }
        actor.sendMessage("File edits§8:")
        type.fileEdits.keys.forEach {
            actor.sendMessage("\t§8- %hc%$it")
            type.fileEdits[it]?.forEach { edit ->
                actor.sendMessage("\t    §8➥ %tc%${edit.key} §8➜ %tc%${edit.value}")
            }
        }
        actor.sendMessage("")
        actor.sendMessage("§8<====== %hc%§nServer-Version type§8 ======§8>")
    }

    @CommandSubPath("delete <name>")
    @CommandDescription("Delete a server version type")
    fun delete(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType
    ) {
        runBlocking {
            val versions = configurationTemplateRepository.getTemplates().mapNotNull {
                if (it.serverVersionId == null) return@mapNotNull null
                serverVersionRepository.getVersion(it.serverVersionId!!)
            }.filter { it.typeId != null }
            if (versions.any { it.typeId == type.uniqueId }) {
                actor.sendMessage("§cYou can't delete a server version type which is used by a server version:")
                actor.sendMessage("§c${
                    versions.filter { it.typeId == type.uniqueId }.joinToString(", ") { it.getDisplayName() ?: "null" }}"
                )
                return@runBlocking
            }
            serverVersionTypeRepository.deleteType(type)
            actor.sendMessage("Successfully deleted server version type %tc%${type.name}")
        }
    }

    @CommandSubPath("edit <name> name <new-name>")
    @CommandDescription("Edit the name of a server version type")
    fun editName(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("new-name") newName: String
    ) {
        runBlocking {
            type.name = newName
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited name of server version type to %tc%${type.name}")
        }
    }

    @CommandSubPath("edit <name> handler <handler>")
    @CommandDescription("Edit the handler of a server version type")
    fun editHandler(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("handler", true, ServerVersionHandlerSuggester::class) newHandler: IServerVersionHandler
    ) {
        runBlocking {
            type.versionHandlerName = newHandler.name
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited handler of server version type to %tc%${type.versionHandlerName}")
        }
    }

    @CommandSubPath("edit <name> craftbukkitbased <state>")
    @CommandAlias(["edit <name> cb <state>", "edit <name> craftbukkit <state>"])
    @CommandDescription("Edit the craftbukkit based of a server version type")
    fun editCraftBukkitBased(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("state") craftBukkitBased: Boolean
    ) {
        runBlocking {
            type.craftBukkitBased = craftBukkitBased
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited craftbukkit based of server version type to %tc%${type.craftBukkitBased}")
        }
    }

    @CommandSubPath("edit <name> proxy <state>")
    @CommandDescription("Edit the proxy of a server version type")
    fun editProxy(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("state") proxy: Boolean
    ) {
        runBlocking {
            type.proxy = proxy
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited proxy of server version type to %tc%${type.proxy}")
        }
    }

    @CommandSubPath("edit <name> jvmargument add <argument>")
    @CommandAlias(["edit <name> jvmarg add <argument>"])
    @CommandDescription("Add a jvm argument to a server version type")
    fun addJvmArgument(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("argument") argument: String
    ) {
        runBlocking {
            type.jvmArguments.add(argument)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully added jvm argument to server version type!")
        }
    }

    @CommandSubPath("edit <name> jvmargument remove <argument>")
    @CommandAlias(["edit <name> jvmarg remove <argument>"])
    @CommandDescription("Remove a jvm argument from a server version type")
    fun removeJvmArgument(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("argument") argument: String
    ) {
        runBlocking {
            type.jvmArguments.remove(argument)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully removed jvm argument from server version type!")
        }
    }

    @CommandSubPath("edit <name> programmargument add <argument>")
    @CommandAlias(["edit <name> progarg add <argument>"])
    @CommandDescription("Add a programm argument to a server version type")
    fun addProgrammArgument(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("argument") argument: String
    ) {
        runBlocking {
            type.programmArguments.add(argument)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully added programm argument to server version type!")
        }
    }

    @CommandSubPath("edit <name> file add <file> <key> <value>")
    @CommandDescription("Add a file edit that should be applied before the server is starts")
    fun addFileEdit(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String,
        @CommandParameter("value") value: String
    ) {
        runBlocking {
            val subMap = type.fileEdits.getOrDefault(file, mutableMapOf())
            subMap[key] = value
            type.fileEdits[file] = subMap
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully added file edit to server version type!")
        }
    }

    @CommandSubPath("edit <name> file remove <file> <key>")
    @CommandDescription("Remove a file edit that should be applied before the server is starts")
    fun removeFileEdit(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String
    ) {
        runBlocking {
            val subMap = type.fileEdits.getOrDefault(file, mutableMapOf())
            subMap.remove(key)
            type.fileEdits[file] = subMap
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully removed file edit from server version type!")
        }
    }

}