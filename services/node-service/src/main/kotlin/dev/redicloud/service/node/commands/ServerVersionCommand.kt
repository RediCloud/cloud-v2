package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.utils.CloudServerVersionType
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.service.base.suggester.CloudServerVersionSuggester
import dev.redicloud.service.base.suggester.ServerVersionSuggester
import kotlinx.coroutines.runBlocking
import java.util.*

@Command("sv")
@CommandAlias(["serverversion", "serverversion"])
@CommandDescription("Configures the server version")
class ServerVersionCommand(
    private val serverVersionRepository: ServerVersionRepository
) : CommandBase() {

    @CommandSubPath("edit <version> name <Name>")
    @CommandDescription("Set the name of the server version")
    fun onEditName(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("name", true) name: String) {
        runBlocking {
            val oldName = version.name
            version.name = name
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated name of $oldName to ${version.name}")
        }
    }

    @CommandSubPath("edit <version> url <URL>")
    @CommandDescription("Set the download url of the server version")
    fun onEditUrl(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("url", true) url: String) {
        runBlocking {
            version.customDownloadUrl = url
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated download url of ${version.name} to ${version.customDownloadUrl}")
        }
    }

    @CommandSubPath("edit <version> type <Type>")
    @CommandDescription("Set the type of the server version")
    fun onEditType(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("type", true) typeName: String) {
        runBlocking {
            val type = CloudServerVersionType.VALUES.firstOrNull { it.name.lowercase() == typeName.lowercase() }
            if (type == null) {
                actor.sendMessage("§cUnknown server version type: $typeName")
                actor.sendMessage("§cValid types: ${CloudServerVersionType.VALUES.joinToString(", ") { it.name.lowercase() }}")
                return@runBlocking
            }
            version.type = type
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated type of ${version.name} to ${type.name}")
        }
    }

    @CommandSubPath("edit <version> libPattern <Patter>")
    @CommandDescription("Set the lib pattern for the files that should be stored after the patch. Set to 'null' to disable the patching")
    fun onEditLibPattern(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("pattern", true) pattern: String) {
        runBlocking {
            version.libPattern = if (pattern != "null") pattern else null
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated lib pattern of ${version.name} to ${version.libPattern}")
        }
    }

    @CommandSubPath("edit <version> version <Version>")
    @CommandDescription("Set the minecraft version of the server version")
    fun onEditMinecraftVersion(
        actor: ConsoleActor,
        @CommandParameter("version", true, CloudServerVersionSuggester::class) version: CloudServerVersion,
        @CommandParameter("mcversion", true, ServerVersionSuggester::class) minecraftVersionName: String) {
        runBlocking {
            val minecraftVersion = ServerVersion.versions().firstOrNull { it.name.lowercase() == minecraftVersionName.lowercase() }
            if (minecraftVersion == null) {
                actor.sendMessage("§cUnknown minecraft version: $minecraftVersionName")
                actor.sendMessage("§cValid versions: ${ServerVersion.versions().joinToString(", ") { it.name.lowercase() }}")
                return@runBlocking
            }
            version.version = minecraftVersion
            serverVersionRepository.updateVersion(version)
            actor.sendMessage("Updated minecraft version of ${version.name} to ${minecraftVersion.name}")
        }
    }

    @CommandSubPath("create <version>")
    @CommandDescription("Create a new server version")
    fun onCreate(
        actor: ConsoleActor,
        @CommandParameter("name", true) versionName: String) {
        runBlocking {
            val version = CloudServerVersion(
                UUID.randomUUID(),
                CloudServerVersionType.UNKNOWN,
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

}