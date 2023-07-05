package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.java.version.getJavaVersion
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.service.base.suggester.*
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.fileName
import dev.redicloud.utils.isValid
import dev.redicloud.utils.isValidUrl
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
) : CommandBase() {

    @CommandSubPath("edit <version> project <name>")
    @CommandDescription("Set the project name of the server version")
    fun onEditName(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("name", true) name: String
    ) {
        runBlocking {
            if (serverVersionRepository.existsVersion(name)) {
                actor.sendMessage("§cA version with the name '$name' already exists!")
                return@runBlocking
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
            if (url != "null" && isValidUrl(url)) {
                actor.sendMessage("§cThe url '$url' is not valid!")
                return@launch
            }
            version.customDownloadUrl = if (url != "null") url else null
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated download url of ${version.getDisplayName()} to ${version.customDownloadUrl}")
        }
    }

    @CommandSubPath("edit <version> type <type>")
    @CommandDescription("Set the type of the server version")
    fun onEditType(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("type", true, CloudServerVersionTypeSuggester::class) type: CloudServerVersionType
    ) {
        runBlocking {
            if (version.version.versionTypes.isNotEmpty()
                && version.version.versionTypes.none { it.lowercase() == type.name.lowercase() }) {
                actor.sendMessage("§cThe type ${toConsoleValue(type.name, false)} is not supported by the version ${toConsoleValue(version.version.name, false)}")
                return@runBlocking
            }
            version.typeId = type.uniqueId
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated type of ${toConsoleValue(version.getDisplayName())} to ${toConsoleValue(type.name)}")
        }
    }

    @CommandSubPath("edit <version> libPattern <pattern>")
    @CommandDescription("Set the lib pattern for the files that should be stored after the patch. Set to 'null' to disable the patching")
    fun onEditLibPattern(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("pattern", true) pattern: String
    ) {
        runBlocking {
            version.libPattern = if (pattern != "null") pattern else null
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated lib pattern of ${toConsoleValue(version.getDisplayName())} to §8'%tc%${version.libPattern}§8'")
        }
    }

    @CommandSubPath("edit <version> javaversion <java>")
    @CommandAlias(["edit <version> jv <java>"])
    @CommandDescription("Edit the Java version of a server version")
    fun editJavaVersion(
        actor: ConsoleActor,
        @CommandParameter("name", true, CloudServerVersionSuggester::class) serverVersion: CloudServerVersion,
        @CommandParameter("java", true, JavaVersionSuggester::class) version: JavaVersion
    ) = runBlocking {
        serverVersion.javaVersionId = version.uniqueId
        serverVersionRepository.updateVersion(serverVersion)
        actor.sendMessage("The java version of the server version ${toConsoleValue(serverVersion.getDisplayName())} was updated to ${toConsoleValue(version.name)}!")
    }

    @CommandSubPath("edit <version> version <mcversion>")
    @CommandDescription("Set the minecraft version of the server version")
    fun onEditMinecraftVersion(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("mcversion", true, ServerVersionSuggester::class) minecraftVersion: ServerVersion
    ) {
        runBlocking {
            version.version = minecraftVersion
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated minecraft version of ${toConsoleValue(version.getDisplayName())} to ${toConsoleValue(minecraftVersion.name)}")
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
        runBlocking {
            if (version.defaultFiles.any { it.key.lowercase() == url.lowercase() }) {
                actor.sendMessage("§cThe file with the url '$url' is already added to the version ${toConsoleValue(version.getDisplayName())}!")
                return@runBlocking
            }
            val u = URL(url)
            if (!u.isValid()) {
                actor.sendMessage("§cThe url '$url' is not valid!")
                return@runBlocking
            }
            version.defaultFiles[url] = path ?: u.fileName
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Added file with url ${toConsoleValue(url)} to ${toConsoleValue(version.getDisplayName())}")
        }
    }

    @CommandSubPath("edit <version> files remove <url>")
    @CommandDescription("Remove a file from the server version that will be downloaded")
    fun onEditFilesRemove(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("url") url: String
    ) {
        runBlocking {
            if (version.defaultFiles.none { it.key.lowercase() == url.lowercase() }) {
                actor.sendMessage("§cThe file with the url '$url' is not added to the version ${toConsoleValue(version.getDisplayName())}!")
                return@runBlocking
            }
            version.defaultFiles.remove(url)
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Removed file with url ${toConsoleValue(url)} from ${toConsoleValue(version.getDisplayName())}")
        }
    }

    @CommandSubPath("create <project> <version>")
    @CommandDescription("Create a new server version")
    fun onCreate(
        actor: ConsoleActor,
        @CommandParameter("project", true) projectName: String,
        @CommandParameter("version", true, ServerVersionSuggester::class) mcVersion: ServerVersion
    ) {
        runBlocking {
            val version = CloudServerVersion(
                UUID.randomUUID(),
                CloudServerVersionTypeRepository.DEFAULT_TYPES_CACHE.get()!!.first { it.name == "unknown" }.uniqueId,
                projectName,
                null,
                null,
                null,
                mcVersion,
                null,
                mutableMapOf()
            )
            if (serverVersionRepository.existsVersion(version.getDisplayName())) {
                actor.sendMessage("§cA server version with the project name $projectName and the mc version ${mcVersion.name }already exists!")
                return@runBlocking
            }
            serverVersionRepository.createVersion(version)
            actor.sendMessage("Created server version with name ${toConsoleValue(version.getDisplayName())}")
            actor.sendMessage("Use '/sv edit ${version.getDisplayName()} <key> <value>' to edit the server version")
        }
    }

    @CommandSubPath("delete <version>")
    @CommandDescription("Delete a server version")
    fun onDelete(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        runBlocking {
            val servers = serverRepository.getConnectedServers()
                .filter { it.configurationTemplate.serverVersionId == version.uniqueId }
            if (servers.isNotEmpty()) {
                actor.sendMessage("§cThere are still servers connected to this version:")
                servers.forEach {
                    actor.sendMessage("§8- %hc%${it.serviceId.toName()}")
                }
                return@runBlocking
            }
            val templates = configurationTemplateRepository.getTemplates()
                .filter { it.serverVersionId == version.uniqueId }
            if (templates.isNotEmpty()) {
                actor.sendMessage("§cThere are still configuration templates using this version:")
                templates.forEach {
                    actor.sendMessage("§8- %hc%${it.name}")
                }
                return@runBlocking
            }
            serverVersionRepository.deleteVersion(version.uniqueId)
            actor.sendMessage("Deleted server version with name ${toConsoleValue(version.getDisplayName())}")
        }
    }

    @CommandSubPath("list")
    @CommandDescription("List all versions")
    fun onCreate(
        actor: ConsoleActor
    ) {
        runBlocking {
            val versions = serverVersionRepository.getVersions()
            if (versions.isEmpty()) {
                actor.sendMessage("§cThere are no server versions")
                return@runBlocking
            }
            actor.sendHeader("Server-Versions")
            actor.sendMessage("")
            actor.sendMessage("Server versions§8:")
            versions.forEach {
                val type = if (it.typeId != null) serverVersionTypeRepository.getType(it.typeId!!) else null
                actor.sendMessage("§8- %hc%${it.getDisplayName()} §8(%tc%${type?.name ?: "unknown"}§8)")
            }
            actor.sendMessage("")
            actor.sendHeader("Server-Versions")
        }
    }

    @CommandSubPath("info <version>")
    @CommandDescription("Get info about a version")
    fun onInfo(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        runBlocking {
            val type = if (version.typeId != null) serverVersionTypeRepository.getType(version.typeId!!) else null
            actor.sendHeader("Server-Version info")
            actor.sendMessage("")
            actor.sendMessage("§8- %tc%Name§8: %hc%${version.getDisplayName()}")
            actor.sendMessage("§8- %tc%Project§8: %hc%${version.projectName}")
            actor.sendMessage("§8- %tc%Type§8: %hc%${type?.name ?: "unknown"}")
            actor.sendMessage("§8- %tc%Lib-Pattern§8: %hc%${version.libPattern ?: "not set"}")
            actor.sendMessage("§8- %tc%Version§8: %hc%${version.version.name}")
            actor.sendMessage("§8- %tc%Version-Handler§8: %hc%${type?.versionHandlerName ?: "unknown"}")
            actor.sendMessage("§8- %tc%Download-Url§8: %hc%${version.customDownloadUrl ?: "not set"}")
            actor.sendMessage("§8- %tc%Build§8: %hc%${version.buildId ?: "not set"}")
            val javaVersion = if (version.javaVersionId != null) javaVersionRepository.getVersion(version.javaVersionId!!) else null
            actor.sendMessage("§8- %tc%Java version§8: %hc%${javaVersion?.name ?: "not set"}")
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
                actor.sendMessage("§cYou can set one with '/sv edit ${version.getDisplayName()} type <type>'")
                return@launch
            }
            val handler = IServerVersionHandler.getHandler(type)
            if (!handler.isPatchVersion(version)) {
                actor.sendMessage("§cThis version is not patchable! Set the lib pattern with '/sv edit ${version.getDisplayName()} libpattern <pattern>'")
                return@launch
            }
            try {
                handler.patch(version)
            } catch (e: Exception) {
                LOGGER.severe("Error while patching version ${version.getDisplayName()}", e)
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
                actor.sendMessage("§c'${version.getDisplayName()}' can´t be downloaded! Check the version type and the download url!")
                return@launch
            }
            try {
                handler.download(version, true)
            } catch (e: Exception) {
                LOGGER.severe("Error while downloading version ${version.getDisplayName()}", e)
            }
        }
    }

}