package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.java.version.getVersionInfo
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.service.base.suggester.JavaVersionSuggester
import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import dev.redicloud.utils.toSymbol
import java.io.File
import java.util.*

@Command("javaversion")
@CommandAlias(["jv", "javaversions"])
@CommandDescription("Manage java versions")
class JavaVersionCommand(
    private val javaVersionRepository: JavaVersionRepository,
    private val serverVersionRepository: CloudServerVersionRepository
) : ICommand {

    @CommandSubPath("list")
    @CommandDescription("List all java versions")
    suspend fun list(
        actor: ConsoleActor
    ) {
        actor.sendHeader("Java-Versions")
        actor.sendMessage("")
        javaVersionRepository.getVersions().forEach {
            actor.sendMessage(
                "§8- %hc%${it.name} §8| %tc%Installed§8: ${it.isLocated(javaVersionRepository.serviceId).toSymbol()}"
            )
        }
        actor.sendMessage("")
        actor.sendHeader("Java-Versions")
    }

    @CommandSubPath("auto-locate")
    @CommandDescription("Auto locate all java versions")
    suspend fun autoLocate(
        actor: ConsoleActor
    ) {
        val versions = javaVersionRepository.detectInstalledVersions()
        if (versions.isEmpty()) {
            actor.sendMessage("§cNo java versions found")
            return
        }
        val created = mutableListOf<CloudJavaVersion>()
        versions.forEach {
            if (javaVersionRepository.existsVersion(it.name)) return@forEach
            javaVersionRepository.createVersion(it)
            created.add(it)
        }
        if (created.isEmpty()) {
            actor.sendMessage("No new java versions found")
            return
        }
        actor.sendMessage("Created new java versions§8: %hc%${created.joinToString("§8, %hc%") { it.name }}")
    }

    @CommandSubPath("delete <version>")
    @CommandDescription("Delete a java version")
    suspend fun delete(
        actor: ConsoleActor,
        @CommandParameter("version", true, JavaVersionSuggester::class) version: CloudJavaVersion
    ) {
        val versions = serverVersionRepository.getVersions()
        if (versions.any { it.uniqueId == version.uniqueId }) {
            actor.sendMessage("§cThere are still server versions using this version:")
            actor.sendMessage(
                "§c${
                    versions.filter { it.javaVersionId == version.uniqueId }.joinToString(", ") { it.displayName }
                }"
            )
            return
        }
        javaVersionRepository.deleteVersion(version)
        actor.sendMessage("Deleted version ${version.name}")
    }

    @CommandSubPath("locate <version> <path>")
    @CommandDescription("Locate an java version on the current node")
    suspend fun locate(
        actor: ConsoleActor,
        @CommandParameter("version", true, JavaVersionSuggester::class) versionName: String,
        @CommandParameter("path", true) path: String
    ) {
        val file = File(path)
        if (file.isFile) {
            actor.sendMessage("§cThe path must be a directory")
        }
        val executable = when (getOperatingSystemType()) {
            OSType.WINDOWS -> File(file, "bin/java.exe")
            else -> File(file, "bin/java")
        }
        if (!executable.exists()) {
            actor.sendMessage("§cThe path does not contain a valid java installation")
            return
        }
        var version = javaVersionRepository.getVersion(versionName)
        if (version == null) {
            val info = getVersionInfo(file.absolutePath)
            version = CloudJavaVersion(
                UUID.randomUUID(),
                file.name,
                info?.versionId ?: -1,
                false,
                mutableMapOf(
                    javaVersionRepository.serviceId.id to file.absolutePath
                ),
                info
            )
            javaVersionRepository.createVersion(version)
            actor.sendMessage("Located version ${toConsoleValue(version.name)} at ${toConsoleValue(path)}")
            return
        }
        version.located[javaVersionRepository.serviceId.id] = file.absolutePath
        javaVersionRepository.updateVersion(version)
        actor.sendMessage("Located version ${toConsoleValue(version.name)} at ${toConsoleValue(path)}")
    }
}
