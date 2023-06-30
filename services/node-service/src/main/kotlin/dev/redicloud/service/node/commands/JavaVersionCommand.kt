package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.runBlocking

@Command("javaversion")
@CommandAlias(["jv", "javaversions"])
@CommandDescription("Manage java versions")
class JavaVersionCommand(
    private val javaVersionRepository: JavaVersionRepository
) : CommandBase() {

    @CommandSubPath("list")
    @CommandDescription("List all java versions")
    fun list(
        actor: ConsoleActor
    ) {
        runBlocking {
            actor.sendMessage("§8<====== %hc%§nJava-Versions§8 ======§8>")
            actor.sendMessage("")
            javaVersionRepository.getVersions().forEach {
                actor.sendMessage(
                    "§8- §7${it.name} §8(§7${it.id}§8) §8| %tc%Installed$8: ${it.isLocated(javaVersionRepository.serviceId).toSymbol()}"
                )
            }
            actor.sendMessage("")
            actor.sendMessage("§8<====== %hc%§nJava-Versions§8 ======§8>")
        }
    }

    @CommandSubPath("auto-locate")
    @CommandDescription("Auto locate all java versions")
    fun autoLocate(
        actor: ConsoleActor
    ) {
        runBlocking {
            val versions = javaVersionRepository.detectInstalledVersions()
            if (versions.isEmpty()) {
                actor.sendMessage("§cNo java versions found")
                return@runBlocking
            }
            versions.forEach {
                if (javaVersionRepository.existsVersion(it.name)) return@forEach
                javaVersionRepository.createVersion(it)
                actor.sendMessage("Created version %hc%${it.name} §8(§7${it.id}§8)")
            }
        }
    }

}