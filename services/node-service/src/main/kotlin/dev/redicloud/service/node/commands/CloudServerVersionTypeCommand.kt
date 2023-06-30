package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.service.base.suggester.CloudServerVersionTypeSuggester
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.runBlocking

@Command("svt")
@CommandAlias(["serverversiontype", "svtype"])
class CloudServerVersionTypeCommand(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
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
        actor.sendMessage("JVM-Arguments§8: %hc%${type.jvmArguments.joinToString("§8, %hc%")}")
        actor.sendMessage("Programm-Arguments§8: %hc%${type.programmArguments.joinToString("§8, %hc%")}")
        actor.sendMessage("File edits§8:")
        type.fileEdits.keys.forEach {
            actor.sendMessage("\t§8- %hc%$it")
            type.fileEdits[it]?.forEach { edit ->
                actor.sendMessage("\t\t§8➥ %hc%${edit.key} §8➜ %hc%${edit.value}")
            }
        }
        actor.sendMessage("")
        actor.sendMessage("§8<====== %hc%§nServer-Version type§8 ======§8>")
    }

}