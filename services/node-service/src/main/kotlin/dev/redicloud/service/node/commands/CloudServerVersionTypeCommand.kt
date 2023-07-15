package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.service.base.suggester.CloudConnectorFileNameSelector
import dev.redicloud.service.base.suggester.CloudServerVersionTypeSuggester
import dev.redicloud.service.base.suggester.ServerVersionHandlerSuggester
import dev.redicloud.utils.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL
import java.util.*

@Command("svt")
@CommandAlias(["serverversiontype", "svtype"])
@CommandDescription("Configure server version types")
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
        actor.sendHeader("Server-Version type")
        actor.sendMessage("")
        actor.sendMessage("Name§8: %hc%${type.name}")
        actor.sendMessage("Handler§8: %hc%${type.versionHandlerName}")
        actor.sendMessage("Default§8: %hc%${type.defaultType.toSymbol()}")
        actor.sendMessage("Proxy§8: %hc%${type.proxy.toSymbol()}")
        actor.sendMessage(
            "Connector plugin§8: %hc%${
                type.connectorPluginName.replace(
                    "%cloud_version%",
                    CLOUD_VERSION
                )
            }"
        )
        actor.sendMessage(
            "Connector download url§8: %hc%${
                type.connectorDownloadUrl?.replace(
                    "%cloud_version%",
                    CLOUD_VERSION
                ) ?: "Not set"
            }"
        )
        actor.sendMessage("Connector folder§8: %hc%${type.connectorFolder}")
        actor.sendMessage("JVM arguments§8:${if (type.jvmArguments.isEmpty()) " %hc%None" else ""}")
        type.jvmArguments.forEach {
            actor.sendMessage("\t§8- %hc%$it")
        }
        actor.sendMessage("Programm parameters§8:${if (type.programmParameters.isEmpty()) " %hc%None" else ""}")
        type.programmParameters.forEach {
            actor.sendMessage("\t§8- %hc%$it")
        }
        actor.sendMessage("File edits§8:${if (type.fileEdits.isEmpty()) " %hc%not set" else ""}")
        type.fileEdits.keys.forEach {
            actor.sendMessage("\t§8- %hc%$it")
            type.fileEdits[it]?.forEach { edit ->
                actor.sendMessage("\t    §8➥ %tc%${edit.key} §8➜ %tc%${edit.value}")
            }
        }
        actor.sendMessage("Default files§8:${if (type.defaultFiles.isEmpty()) " %hc%not set" else ""}")
        type.defaultFiles.keys.forEach {
            actor.sendMessage("\t§8- %hc%$it §8➜ %tc%${type.defaultFiles[it]}")
        }
        actor.sendMessage("")
        actor.sendHeader("Server-Version type")
    }

    @CommandSubPath("edit <type> files add <url> [path]")
    @CommandDescription("Add a file to the server version type that will be downloaded")
    fun onEditFilesAdd(
        actor: ConsoleActor,
        @CommandParameter("type", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("url") url: String,
        @CommandParameter("path", false) path: String?
    ) {
        runBlocking {
            if (!isValidUrl(url)) {
                actor.sendMessage("§cThe url '$url' is not valid!")
                return@runBlocking
            }
            val file = path ?: URL(url).fileName
            if (type.defaultFiles.any { it.value.lowercase() == file.lowercase() }) {
                actor.sendMessage(
                    "§cThe file with the url ${
                        toConsoleValue(
                            url,
                            false
                        )
                    } was already added to the version ${toConsoleValue(type.name, false)}!"
                )
                return@runBlocking
            }
            type.defaultFiles[url] = file
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Added file with url ${toConsoleValue(url)} to ${toConsoleValue(type.name)}")
        }
    }

    @CommandSubPath("edit <version> libPattern <pattern>")
    @CommandDescription("Set the lib pattern for the files that should be stored after the patch. Set to 'null' to disable the patching")
    fun onEditLibPattern(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("pattern", true) pattern: String
    ) {
        runBlocking {
            type.libPattern = if (pattern != "null") pattern else null
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Updated lib pattern of ${toConsoleValue(type.name)} to ${toConsoleValue(type.libPattern!!)}")
        }
    }

    @CommandSubPath("edit <type> files remove <url>")
    @CommandDescription("Remove a file from the server version type that will be downloaded")
    fun onEditFilesRemove(
        actor: ConsoleActor,
        @CommandParameter("type", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("url") url: String
    ) {
        runBlocking {
            if (type.defaultFiles.none { it.value.lowercase() == url.lowercase() }) {
                actor.sendMessage("§cThe file with the url '$url' is not added to the version ${toConsoleValue(type.name)}!")
                return@runBlocking
            }
            type.defaultFiles.remove(url)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Removed file with url ${toConsoleValue(url)} from ${toConsoleValue(type.name)}")
        }
    }

    @CommandSubPath("create <name>")
    @CommandDescription("Create a new server version type")
    fun create(
        actor: ConsoleActor,
        @CommandParameter("name") name: String
    ) {
        runBlocking {
            if (serverVersionTypeRepository.existsType(name)) {
                actor.sendMessage("§cA server version type with this name already exists!")
                return@runBlocking
            }
            val type = CloudServerVersionType(
                UUID.randomUUID(),
                name,
                "urldownloader",
                false,
                false,
                "redicloud-$name-%cloud_version%-%build_number%.jar",
                null,
                "plugins"
            )
            serverVersionTypeRepository.createType(type)
            actor.sendMessage("Successfully created server version type ${toConsoleValue(type.name)}")
        }
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
                    versions.filter { it.typeId == type.uniqueId }.joinToString(", ") { it.getDisplayName() }
                }"
                )
                return@runBlocking
            }
            if (type.defaultType) {
                actor.sendMessage("§cYou can't delete the default server version type!")
                return@runBlocking
            }
            serverVersionTypeRepository.deleteType(type)
            actor.sendMessage("Successfully deleted server version type ${toConsoleValue(type.name)}")
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
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.name = newName
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited name of server version type to ${toConsoleValue(type.name)}")
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
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.versionHandlerName = newHandler.name
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited handler of server version type to ${toConsoleValue(type.versionHandlerName)}")
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
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.proxy = proxy
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited proxy of server version type to ${toConsoleValue(type.proxy)}")
        }
    }

    @CommandSubPath("edit <name> connector <file>")
    @CommandDescription("Edit the file name of the connector of a server version type")
    fun editConnector(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("file", true, CloudConnectorFileNameSelector::class) connector: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            val oldName = type.connectorPluginName
            val file = File(CONNECTORS_FOLDER.getFile(), oldName)
            if (file.exists()) {
                file.renameTo(File(CONNECTORS_FOLDER.getFile(), connector))
            }
            type.connectorPluginName = connector
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited connector of server version type to ${toConsoleValue(type.connectorPluginName)}")
        }
    }

    @CommandSubPath("edit <name> connectorurl <url>")
    @CommandDescription("Edit the connector download url of a server version type")
    fun editConnectorUrl(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("url", true) url: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            if (!isValidUrl(url)) {
                actor.sendMessage("§cThe url is not valid!")
                return@runBlocking
            }
            type.connectorDownloadUrl = url
            serverVersionTypeRepository.downloadConnector(type, true)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage(
                "Successfully edited connector download url of server version type to ${
                    toConsoleValue(
                        type.connectorDownloadUrl!!
                    )
                }"
            )
        }
    }

    @CommandSubPath("edit <name> connector <folder>")
    @CommandDescription("Edit the connector folder of a server version type. E.g. 'plugins' or 'extensions'")
    fun editConnectorFolder(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("folder", true) folder: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.connectorFolder = folder
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully edited connector folder of server version type to ${toConsoleValue(type.connectorFolder)}")
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
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
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
        @CommandParameter("parameter") argument: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.jvmArguments.remove(argument)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully removed jvm parameter from server version type!")
        }
    }

    @CommandSubPath("edit <name> programmparameter add <parameter>")
    @CommandDescription("Add a programm parameter to a server version type")
    fun addProgrammParameter(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("parameter") parameter: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.programmParameters.add(parameter)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully added programm parameter to server version type!")
        }
    }

    @CommandSubPath("edit <name> programmparameter remove <parameter>")
    @CommandDescription("Remove a programm parameter to a server version type")
    fun removeProgrammParameter(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("parameter") parameter: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            type.programmParameters.remove(parameter)
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully removed the programm parameter from server version type!")
        }
    }

    @CommandSubPath("edit <name> fileedits add <file> <key> <value>")
    @CommandAlias(["edit <name> fe add <file> <key> <value>"])
    @CommandDescription("Add a file edit that should be applied before the server is starts")
    fun addFileEdit(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String,
        @CommandParameter("value") value: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            val subMap = type.fileEdits.getOrDefault(file, mutableMapOf())
            subMap[key] = value
            if (subMap.isEmpty()) {
                type.fileEdits.remove(file)
            } else {
                type.fileEdits[file] = subMap
            }
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully added file edit to server version type!")
        }
    }

    @CommandSubPath("edit <name> fileedits remove <file> <key>")
    @CommandAlias(["edit <name> fe remove <file> <key>"])
    @CommandDescription("Remove a file edit that should be applied before the server is starts")
    fun removeFileEdit(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String
    ) {
        runBlocking {
            if (type.defaultType) {
                actor.sendMessage("§cYou can't edit the name of the default server version type!")
                return@runBlocking
            }
            val subMap = type.fileEdits.getOrDefault(file, mutableMapOf())
            subMap.remove(key)
            type.fileEdits[file] = subMap
            serverVersionTypeRepository.updateType(type)
            actor.sendMessage("Successfully removed file edit from server version type!")
        }
    }

}