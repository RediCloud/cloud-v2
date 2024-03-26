package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.Console
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.repository.server.version.serverversion.ServerVersion
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.service.base.suggester.*
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.fileName
import dev.redicloud.utils.isValidUrl
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.launch
import java.net.URL
import java.util.*

@Command("sv")
@CommandAlias(["serverversion", "serverversion"])
@CommandDescription("Configures the server version")
class CloudServerVersionCommand(
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val serverRepository: ServerRepository,
    private val javaVersionRepository: JavaVersionRepository,
    private val console: Console
) : ICommand {


    @CommandSubPath("duplicate <version> [new-name]")
    @CommandDescription("Duplicate a server version")
    fun duplicate(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("newName", false) newName: String?
    ) = defaultScope.launch {
        val newVersion = version.copy(newName ?: "${version.projectName}_copy")
        if (serverVersionRepository.existsVersion(newVersion.displayName)) {
            actor.sendMessage("§cA version with the name '${newVersion.displayName}' already exists!")
            return@launch
        }
        serverVersionRepository.createVersion(newVersion)
        actor.sendMessage("Duplicated server version ${toConsoleValue(version.displayName)} to ${toConsoleValue(newVersion.displayName)}")
    }

    @CommandSubPath("edit <version> project <name>")
    @CommandDescription("Set the project name of the server version")
    fun onEditName(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("name", true) name: String
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            if (serverVersionRepository.existsVersion(name)) {
                actor.sendMessage("§cA version with the name '$name' already exists!")
                return@launch
            }
            val oldName = version.projectName
            version.projectName = name
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated project name of $oldName to ${version.projectName}")
        }
    }

    @CommandSubPath("edit <version> url <url>")
    @CommandDescription("Set the download url of the server version")
    fun onEditUrl(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("url", true) url: String
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            if (url != "null" && !isValidUrl(url)) {
                actor.sendMessage("§cThe url '$url' is not valid!")
                return@launch
            }
            version.customDownloadUrl = if (url != "null") url else null
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated download url of ${toConsoleValue(version.displayName)} to ${toConsoleValue(url)}")
        }
    }

    @CommandSubPath("edit <version> type <type>")
    @CommandDescription("Set the type of the server version")
    fun onEditType(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("type", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            if (version.version.versionTypes.isNotEmpty()
                && version.version.versionTypes.none { it.lowercase() == type.name.lowercase() }
                && version.version.versionTypes.filter { it.startsWith("!") }
                    .any { it.replaceFirst("!", "").lowercase() == type.name.lowercase() }
            ) {
                actor.sendMessage(
                    "§cThe type ${
                        toConsoleValue(
                            type.name,
                            false
                        )
                    } is not supported by the version ${toConsoleValue(version.version.name, false)}"
                )
                return@launch
            }
            version.typeId = type.uniqueId
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated type of ${toConsoleValue(version.displayName)} to ${toConsoleValue(type.name)}")
        }
    }

    @CommandSubPath("edit <version> libPattern <pattern>")
    @CommandDescription("Set the lib pattern for the files that should be stored after the patch. Set to 'null' to disable the patching")
    fun onEditLibPattern(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("pattern", true) pattern: String
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            version.libPattern = if (pattern != "null") pattern else null
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated lib pattern of ${toConsoleValue(version.displayName)} to §8'%tc%${version.libPattern}§8'")
        }
    }

    @CommandSubPath("edit <version> patch <state>")
    @CommandDescription("Enable or disable the patching of the server version")
    fun onEditPatch(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("state", true, BooleanSuggester::class) state: Boolean
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            version.patch = state
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated patching of ${toConsoleValue(version.displayName)} to ${toConsoleValue(state)}")
        }
    }

    @CommandSubPath("edit <version> javaversion <java>")
    @CommandAlias(["edit <version> jv <java>"])
    @CommandDescription("Edit the Java version of a server version")
    fun editJavaVersion(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) serverVersion: CloudServerVersion,
        @CommandParameter("java", true, JavaVersionSuggester::class) version: CloudJavaVersion
    ) = defaultScope.launch {
        serverVersion.javaVersionId = version.uniqueId
        serverVersionRepository.updateVersion(serverVersion)
        actor.sendMessage(
            "The java version of the server version ${toConsoleValue(serverVersion.displayName)} was updated to ${
                toConsoleValue(
                    version.name
                )
            }!"
        )
    }

    @CommandSubPath("edit <version> version <mcversion>")
    @CommandDescription("Set the minecraft version of the server version")
    fun onEditMinecraftVersion(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("mcversion", true, ServerVersionSuggester::class) minecraftVersion: ServerVersion
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            version.version = minecraftVersion
            serverVersionRepository.updateVersion(version)
            actor.sendMessage(
                "Updated minecraft version of ${toConsoleValue(version.displayName)} to ${
                    toConsoleValue(
                        minecraftVersion.name
                    )
                }"
            )
        }
    }

    @CommandSubPath("edit <name> jvmargument add <argument>")
    @CommandAlias(["edit <name> jvmarg add <argument>"])
    @CommandDescription("Add a jvm argument to the server version")
    fun addJvmArgument(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) type: CloudServerVersion,
        @CommandParameter("argument") argument: String
    ) {
        defaultScope.launch {
            type.jvmArguments.add(argument)
            serverVersionRepository.updateVersion(type)
            actor.sendMessage(
                "The JVM argument ${toConsoleValue(argument)} was added to the server version ${
                    toConsoleValue(
                        type.displayName
                    )
                }!"
            )
        }
    }

    @CommandSubPath("edit <name> jvmargument remove <argument>")
    @CommandAlias(["edit <name> jvmarg remove <argument>"])
    @CommandDescription("Remove a jvm argument from the server version")
    fun removeJvmArgument(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) type: CloudServerVersion,
        @CommandParameter("argument") argument: String
    ) {
        defaultScope.launch {
            type.jvmArguments.remove(argument)
            serverVersionRepository.updateVersion(type)
            actor.sendMessage(
                "The JVM argument ${toConsoleValue(argument)} was removed from the server version ${
                    toConsoleValue(
                        type.displayName
                    )
                }!"
            )
        }
    }

    @CommandSubPath("edit <name> fileedits add <file> <key>")
    @CommandAlias(["edit <name> fe add <file> <key>"])
    @CommandDescription("Add a file edit that should be applied before the server is starts")
    fun addFileEdit(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String,
        @CommandParameter("value") value: String
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            val subMap = version.fileEdits.getOrDefault(file, mutableMapOf())
            subMap[key] = value
            version.fileEdits[file] = subMap
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Successfully added file edit to server version type!")
        }
    }

    @CommandSubPath("edit <name> fileedits remove <file> <key>")
    @CommandAlias(["edit <name> fe remove <file> <key>"])
    @CommandDescription("Remove a file edit that should be applied before the server is starts")
    fun removeFileEdit(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("file") file: String,
        @CommandParameter("key") key: String
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            val subMap = version.fileEdits.getOrDefault(file, mutableMapOf())
            subMap.remove(key)
            if (subMap.isEmpty()) {
                version.fileEdits.remove(file)
            } else {
                version.fileEdits[file] = subMap
            }
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Successfully removed file edit from server version type!")
        }
    }

    @CommandSubPath("edit <version> files add <url> [path]")
    @CommandDescription("Add a file to the server version that will be downloaded")
    fun onEditFilesAdd(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("url") url: String,
        @CommandParameter("path", false) path: String?
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            if (!isValidUrl(url)) {
                actor.sendMessage("§cThe url '$url' is not valid!")
                return@launch
            }
            val file = path ?: URL(url).fileName
            if (version.defaultFiles.any { it.value.lowercase() == file.lowercase() }) {
                actor.sendMessage(
                    "§cThe file with the url '$url' is already added to the version ${
                        toConsoleValue(
                            version.displayName
                        )
                    }!"
                )
                return@launch
            }
            version.defaultFiles[url] = file
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Added file with url ${toConsoleValue(url)} to ${toConsoleValue(version.displayName)}")
        }
    }

    @CommandSubPath("edit <version> files remove <url>")
    @CommandDescription("Remove a file from the server version that will be downloaded")
    fun onEditFilesRemove(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("url") url: String
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            if (version.defaultFiles.none { it.value.lowercase() == url.lowercase() }) {
                actor.sendMessage("§cThe file with the url '$url' is not added to the version ${toConsoleValue(version.displayName)}!")
                return@launch
            }
            version.defaultFiles.remove(url)
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Removed file with url ${toConsoleValue(url)} from ${toConsoleValue(version.displayName)}")
        }
    }

    @CommandSubPath("edit <name> programparameter add <parameter>")
    @CommandDescription("Add a program parameter to the version")
    fun addProgramParameter(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("parameter") parameter: String
    ) {
        defaultScope.launch {
            if (version.programParameters.contains(parameter)) {
                actor.sendMessage(
                    "§cThe program parameter '$parameter' is already added to the version ${
                        toConsoleValue(
                            version.displayName
                        )
                    }!"
                )
                return@launch
            }
            version.programParameters.add(parameter)
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Added program parameter ${toConsoleValue(parameter)} to ${toConsoleValue(version.displayName)}")
        }
    }

    @CommandSubPath("edit <name> programparameter remove <parameter>")
    @CommandDescription("Remove a program parameter from the version")
    fun removeProgramParameter(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("parameter") parameter: String
    ) {
        defaultScope.launch {
            if (!version.programParameters.contains(parameter)) {
                actor.sendMessage(
                    "§cThe program parameter '$parameter' is not added to the version ${
                        toConsoleValue(
                            version.displayName
                        )
                    }!"
                )
                return@launch
            }
            version.programParameters.remove(parameter)
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Removed program parameter ${toConsoleValue(parameter)} from ${toConsoleValue(version.displayName)}")
        }
    }


    @CommandSubPath("create <project> <version>")
    @CommandDescription("Create a new server version")
    fun onCreate(
        actor: ConsoleActor,
        @CommandParameter("project", true) projectName: String,
        @CommandParameter("version", true, ServerVersionSuggester::class) mcVersion: ServerVersion
    ) {
        defaultScope.launch {
            val version = CloudServerVersion(
                UUID.randomUUID(),
                CloudServerVersionTypeRepository.DEFAULT_TYPES_CACHE.get()!!.first { it.name == "unknown" }.uniqueId,
                projectName,
                null,
                null,
                mcVersion,
                null,
            )
            if (serverVersionRepository.existsVersion(version.displayName)) {
                actor.sendMessage("§cA server version with the project name $projectName and the mc version ${mcVersion.name}already exists!")
                return@launch
            }
            serverVersionRepository.createVersion(version)
            actor.sendMessage("Created server version with name ${toConsoleValue(version.displayName)}")
            actor.sendMessage("Use '/sv edit ${version.displayName} <key> <value>' to edit the server version")
        }
    }

    @CommandSubPath("delete <version>")
    @CommandDescription("Delete a server version")
    fun onDelete(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        defaultScope.launch {
            if (version.online) {
                actor.sendMessage(
                    "§cThe version ${
                        toConsoleValue(
                            version.displayName,
                            false
                        )
                    } is online and can't be edited!"
                )
                return@launch
            }
            val servers = serverRepository.getConnectedServers()
                .filter { it.configurationTemplate.serverVersionId == version.uniqueId }
            if (servers.isNotEmpty()) {
                actor.sendMessage("§cThere are still servers connected to this version:")
                servers.forEach {
                    actor.sendMessage("§8- %hc%${it.serviceId.toName()}")
                }
                return@launch
            }
            val templates = configurationTemplateRepository.getTemplates()
                .filter { it.serverVersionId == version.uniqueId }
            if (templates.isNotEmpty()) {
                actor.sendMessage("§cThere are still configuration templates using this version:")
                templates.forEach {
                    actor.sendMessage("§8- %hc%${it.name}")
                }
                return@launch
            }
            serverVersionRepository.deleteVersion(version.uniqueId)
            actor.sendMessage("Deleted server version with name ${toConsoleValue(version.displayName)}")
        }
    }

    @CommandSubPath("list")
    @CommandDescription("List all versions")
    fun onCreate(
        actor: ConsoleActor
    ) {
        defaultScope.launch {
            val versions = serverVersionRepository.getVersions()
            if (versions.isEmpty()) {
                actor.sendMessage("§cThere are no server versions")
                return@launch
            }
            val unknown = serverVersionTypeRepository.getOnlineTypes().first { it.isUnknown() }
            val types = mutableMapOf<UUID, CloudServerVersionType>()
            val map = mutableMapOf<UUID, MutableList<CloudServerVersion>>()
            versions.forEach {
                val type =
                    if (it.typeId != null) serverVersionTypeRepository.getType(it.typeId!!) ?: unknown else unknown
                val list = map.getOrDefault(type.uniqueId, mutableListOf())
                list.add(it)
                map[type.uniqueId] = list
                types[type.uniqueId] = type
            }
            actor.sendHeader("Server-Versions")
            actor.sendMessage("")
            map.forEach {
                val type = types[it.key]!!
                val vs = it.value.sortedBy { it }
                actor.sendMessage("§8- %hc%§n${type.name} §8§n(%tc%§n${vs.size}§8§n):")
                var count = 0
                var line = StringBuilder()
                vs.forEach { version ->
                    if (count == 5) {
                        actor.sendMessage(line.toString())
                        line = StringBuilder()
                        count = 0
                    }
                    count++
                    if (line.isEmpty()) {
                        line.append("\t   §8➥ ")
                    } else {
                        line.append("§8, ")
                    }
                    val displayName = "%tc%${version.projectName}§8_%hc%${version.version.name}"
                    line.append("${displayName}${if (version.version.latest) " §8(%tc%${version.version.dynamicVersion().name}§8)" else ""}")
                }
                if (line.isNotEmpty()) {
                    actor.sendMessage(line.toString())
                }
                actor.sendMessage("")
            }
            actor.sendHeader("Server-Versions")
        }
    }

    @CommandSubPath("info <version>")
    @CommandDescription("Get info about a version")
    fun onInfo(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        defaultScope.launch {
            val type = if (version.typeId != null) serverVersionTypeRepository.getType(version.typeId!!) else null
            actor.sendHeader("Server-Version info")
            actor.sendMessage("")
            actor.sendMessage("§8- %tc%Name§8: %hc%${version.displayName}")
            actor.sendMessage("§8- %tc%Project§8: %hc%${version.projectName}")
            actor.sendMessage("§8- %tc%Type§8: %hc%${type?.name ?: "unknown"}")
            actor.sendMessage("§8- %tc%Lib-Pattern§8: %hc%${version.libPattern ?: "not set"}")
            actor.sendMessage("§8- %tc%Online§8: %hc%${version.online.toSymbol()}")
            actor.sendMessage("§8- %tc%Used§8: %hc%${version.used.toSymbol()}")
            actor.sendMessage("§8- %tc%Version§8: %hc%${version.version.name}")
            actor.sendMessage("§8- %tc%Version-Id§8: %hc%${if (version.version.latest) "latest (${version.version.dynamicVersion().name})" else version.version.name}")
            actor.sendMessage("§8- %tc%Patch-Version§8: ${version.patch.toSymbol()}")
            actor.sendMessage("§8- %tc%Version-Handler§8: %hc%${type?.versionHandlerName ?: "unknown"}")
            actor.sendMessage("§8- %tc%Download-Url§8: %hc%${version.customDownloadUrl ?: "not set"}")
            actor.sendMessage("§8- %tc%Build§8: %hc%${version.buildId ?: "not set"}")
            val javaVersion =
                if (version.javaVersionId != null) javaVersionRepository.getVersion(version.javaVersionId!!) else null
            actor.sendMessage("§8- %tc%Java version§8: %hc%${javaVersion?.name ?: "not set"}")
            actor.sendMessage("§8- %tc%Default-Files§8:${if (version.defaultFiles.isEmpty()) " %hc%not set" else ""}")
            version.defaultFiles.forEach {
                actor.sendMessage("§8  - %hc%${it.key} §8➔ %tc%${it.value}")
            }
            actor.sendMessage("§8- %tc%JVM arguments§8:${if (version.jvmArguments.isEmpty()) " %hc%not set" else ""}")
            version.jvmArguments.forEach {
                actor.sendMessage("§8  - %hc%$it")
            }
            actor.sendMessage("§8- %tc%Program parameters§8:${if (version.programParameters.isEmpty()) " %hc%not set" else ""}")
            version.programParameters.forEach {
                actor.sendMessage("§8  - %hc%$it")
            }
            actor.sendMessage("§8- %tc%File edits§8:${if (version.fileEdits.isEmpty()) " %hc%not set" else ""}")
            version.fileEdits.keys.forEach {
                actor.sendMessage("\t§8- %hc%$it")
                version.fileEdits[it]?.forEach { edit ->
                    actor.sendMessage("\t   §8➥ %tc%${edit.key} §8➜ %tc%${edit.value}")
                }
            }
            actor.sendMessage("")
            actor.sendHeader("Server-Version info")
        }
    }

    @CommandSubPath("patch <version>")
    @CommandDescription("Patch a server version")
    fun onPatch(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        defaultScope.launch {
            val type = if (version.typeId != null) serverVersionTypeRepository.getType(version.typeId!!) else null
            if (type == null) {
                actor.sendMessage("§cThis version has no server version type!")
                actor.sendMessage("§cYou can set one with '/sv edit ${version.displayName} type <type>'")
                return@launch
            }
            val handler = IServerVersionHandler.getHandler(type)
            if (!handler.isPatchVersion(version)) {
                actor.sendMessage("§cThis version is not patchable! Set the lib pattern with '/sv edit ${version.displayName} patchh true'")
                return@launch
            }
            try {
                handler.patch(version)
            } catch (e: Exception) {
                LOGGER.severe("Error while patching version ${version.displayName}", e)
            }
        }
    }

    @CommandSubPath("download <version>")
    @CommandDescription("Download the server version")
    fun onDownload(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        defaultScope.launch {
            val type = if (version.typeId != null) serverVersionTypeRepository.getType(version.typeId!!) else null
            if (type == null) {
                actor.sendMessage("§cThis version has no server version type!")
                actor.sendMessage("§cYou can set one with '/sv edit ${version.projectName} type <type>'")
                return@launch
            }
            val handler = IServerVersionHandler.getHandler(type)
            if (!handler.canDownload(version)) {
                actor.sendMessage("§c'${version.displayName}' can´t be downloaded! Check the version type and the download url!")
                return@launch
            }
            try {
                handler.download(version, true)
            } catch (e: Exception) {
                LOGGER.severe("Error while downloading version ${version.displayName}", e)
            }
        }
    }

}