package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.Console
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.service.base.suggester.CloudServerVersionSuggester
import dev.redicloud.service.base.suggester.CloudServerVersionTypeSuggester
import dev.redicloud.service.base.suggester.ServerVersionSuggester
import dev.redicloud.service.node.repository.node.LOGGER
import kotlinx.coroutines.runBlocking
import java.util.*

@Command("sv")
@CommandAlias(["serverversion", "serverversion"])
@CommandDescription("Configures the server version")
class CloudServerVersionCommand(
    private val serverVersionRepository: ServerVersionRepository,
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val serverRepository: ServerRepository,
    private val console: Console
) : CommandBase() {

    @CommandSubPath("edit <version> name <name>")
    @CommandDescription("Set the name of the server version")
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
            val oldName = version.name
            version.name = name
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated name of $oldName to ${version.name}")
        }
    }

    @CommandSubPath("edit <version> url <url>")
    @CommandDescription("Set the download url of the server version")
    fun onEditUrl(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("url", true) url: String
    ) {
        runBlocking {
            version.customDownloadUrl = if (url != "null") url else null
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated download url of ${version.name} to ${version.customDownloadUrl}")
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
            version.type = type
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated type of ${version.name} to ${type.name}")
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
            actor.sendMessage("Updated lib pattern of ${version.name} to '${version.libPattern}'")
        }
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
            actor.sendMessage("Updated minecraft version of ${version.name} to ${minecraftVersion.name}")
        }
    }

    @CommandSubPath("create <name>")
    @CommandDescription("Create a new server version")
    fun onCreate(
        actor: ConsoleActor,
        @CommandParameter("name", true) versionName: String
    ) {
        runBlocking {
            if (serverVersionRepository.existsVersion(versionName)) {
                actor.sendMessage("§cA server version with the name $versionName already exists!")
                return@runBlocking
            }
            val version = CloudServerVersion(
                UUID.randomUUID(),
                CloudServerVersionTypeRepository.DEFAULT_TYPES_CACHE.get()!!.first { it.name == "unknown" },
                versionName,
                null,
                null,
                null,
                ServerVersion.versions().first { it.isUnknown() }
            )
            serverVersionRepository.createVersion(version)
            actor.sendMessage("§aCreated server version with name ${version.name}")
            actor.sendMessage("Use '/sv edit ${version.name} <key> <value>' to edit the server version")
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
            actor.sendMessage("Deleted server version with name ${version.name}")
        }
    }

    @CommandSubPath("list")
    @CommandDescription("List all versions")
    fun onCreate(
        actor: ConsoleActor
    ) {
        runBlocking {
            actor.sendMessage("Server versions:")
            serverVersionRepository.getVersions().forEach {
                actor.sendMessage("§8- %hc%${it.name} §8(%tc%${it.type.name}§8)")
            }
        }
    }

    @CommandSubPath("info <version>")
    @CommandDescription("Get info about a version")
    fun onInfo(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        actor.sendMessage("§8<====== %hc%§nServer-Version info §8 ======§8>")
        actor.sendMessage("")
        actor.sendMessage("§8- %hc%Name§8: %tc%${version.name}")
        actor.sendMessage("§8- %hc%Type§8: %tc%${version.type.name}")
        actor.sendMessage("§8- %hc%Lib-Pattern§8: %tc%${version.libPattern}")
        actor.sendMessage("§8- %hc%Version§8: %tc%${version.version.name}")
        actor.sendMessage("§8- %hc%Download-Url§8: %tc%${version.customDownloadUrl}")
        actor.sendMessage("")
        actor.sendMessage("§8<====== %hc%§nServer-Version info §8 ======§8>")
    }

    @CommandSubPath("patch <version>")
    @CommandDescription("Patch a server version")
    fun onPatch(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        runBlocking {
            val handler = version.getHandle()
            if (!handler.isPatchVersion(version)) {
                actor.sendMessage("§cThis version is not patchable!")
                return@runBlocking
            }
            var canceled = false
            var patched = false
            var error = false
            val animation = AnimatedLineAnimation(
                console,
                200
            ) {
                if (canceled) {
                    null
                } else if (patched) {
                    canceled = true
                    "Patching version %tc%${version.name}§8: ${if (error) "§4✘" else "§a✔"}"
                } else {
                    "Patching version %tc%${version.name}§8: %loading%"
                }
            }
            try {
                handler.patch(version)
            } catch (e: Exception) {
                error = true
                LOGGER.severe("Error while patching version ${version.name}", e)
            } finally {
                patched = true
            }
        }
    }

    @CommandSubPath("download <version>")
    @CommandDescription("Download the server version")
    fun onDownload(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion
    ) {
        runBlocking {
            val handler = version.getHandle()
            if (handler.canDownload(version)) {
                actor.sendMessage("§c'${version.name}' can´t be downloaded! Check the version type and the download url!")
                return@runBlocking
            }
            var canceled = false
            var downloaded = false
            var error = false
            val animation = AnimatedLineAnimation(
                console,
                200
            ) {
                if (canceled) {
                    null
                } else if (downloaded) {
                    canceled = true
                    "Downloading version %tc%${version.name}§8: ${if (error) "§4✘" else "§a✔"}"
                } else {
                    "Downloading version %tc%${version.name}§8: %loading%"
                }
            }
            try {
                handler.download(version, true)
            } catch (e: Exception) {
                error = true
                LOGGER.severe("Error while downloading version ${version.name}", e)
            } finally {
                downloaded = true
            }
        }
    }

}