package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.service.base.suggester.CloudServerSuggester
import dev.redicloud.service.base.suggester.ConfigurationTemplateSuggester
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Command("server")
@CommandAlias(["s", "servers"])
@CommandDescription("Manage the servers")
class ServerCommand(
    private val serverFactory: ServerFactory,
    private val serverRepository: ServerRepository,
    private val nodeRepository: NodeRepository
) : CommandBase() {

    @CommandSubPath("list")
    @CommandDescription("List all registered servers")
    fun list(
        actor: ConsoleActor
    ) = runBlocking {
        val registered = serverRepository.getRegisteredServers()
        if (registered.isEmpty()) {
            actor.sendMessage("No servers are registered!")
            return@runBlocking
        }
        actor.sendHeader("Registered servers")
        actor.sendMessage("")
        registered.forEach {
            actor.sendMessage("§8- %hc%${it.name}§8: ${it.state.color}● §8(%tc%${it.state.displayName}%8)")
        }
        actor.sendMessage("")
        actor.sendHeader("Registered servers")
    }

    @CommandSubPath("start <template> [count]")
    @CommandDescription("Queue a amount of servers with a configuration template")
    fun start(
        actor: ConsoleActor,
        @CommandParameter("template", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", false) count: Int = 1
    ) = defaultScope.launch {
        actor.sendMessage("Queued ${toConsoleValue(count)} servers with template ${toConsoleValue(template.name)}...")
        serverFactory.queueStart(template, count)
    }

    @CommandSubPath("stop <server> [force]")
    @CommandDescription("Stop a server")
    fun stop(
        actor: ConsoleActor,
        @CommandParameter("server", true) server: String,
        @CommandParameter("force", false, BooleanSuggester::class) force: Boolean?
    ) = defaultScope.launch {
        val servers = mutableListOf<CloudServer>()
        if (server == "*") {
            serverRepository.getConnectedServers().forEach {
                servers.add(it)
            }
            if (servers.isEmpty()) {
                actor.sendMessage("No servers are connected!")
                return@launch
            }
            actor.sendMessage("Stopping all servers...")
            servers.forEach {
                serverFactory.queueStop(it.serviceId, force ?: false)
            }
            return@launch
        } else if (server.endsWith("*")) {
            val name = server.substring(0, server.length - 1)
            serverRepository.getConnectedServers().forEach {
                if (it.name.lowercase().startsWith(name.lowercase())) {
                    servers.add(it)
                }
            }
            if (servers.isEmpty()) {
                actor.sendMessage("No server starts with ${toConsoleValue(name)}!")
                return@launch
            }
            actor.sendMessage("Stopping all servers starting with ${toConsoleValue(name)}...")
            servers.forEach {
                serverFactory.queueStop(it.serviceId, force ?: false)
            }
            return@launch
        } else if (server.contains(",")) {
            val names = server.split(",")
            names.forEach {
                val name = it.trim()
                serverRepository.getConnectedServers().forEach { server ->
                    if (server.name.lowercase() == name.lowercase()) {
                        servers.add(server)
                    }
                }
            }
            actor.sendMessage("Stopping ${toConsoleValue(servers.size)} servers...")
            servers.forEach {
                serverFactory.queueStop(it.serviceId, force ?: false)
            }
            return@launch
        } else {
            serverRepository.getConnectedServers().forEach {
                if (it.name.lowercase() == server.lowercase()) {
                    servers.add(it)
                }
            }
            if (servers.isEmpty()) {
                actor.sendMessage("No server with name ${toConsoleValue(server)} found!")
                return@launch
            }
            actor.sendMessage("Stopping server ${toConsoleValue(server)}...")
            servers.forEach {
                serverFactory.queueStop(it.serviceId, force ?: false)
            }
            return@launch
        }
    }

    @CommandSubPath("info <server>")
    @CommandDescription("Get information about a server")
    fun info(
        actor: ConsoleActor,
        @CommandParameter("server", true, CloudServerSuggester::class) server: CloudServer
    ) = runBlocking {
        actor.sendHeader("Server information")
        actor.sendMessage("")
        actor.sendMessage("§8- %tc%Name§8: %hc%${server.name}")
        actor.sendMessage("§8- %tc%ID§8: %hc%${server.serviceId.id}")
        actor.sendMessage("§8- %tc%State§8: ${server.state.color}● §8(%tc%${server.state.displayName}%8)")
        actor.sendMessage("§8- %tc%Configuration template§8: %hc%${server.configurationTemplate.name}")
        val node = nodeRepository.getNode(server.hostNodeId)
        actor.sendMessage("§8- %tc%Node§8: %hc%${node?.name ?: "unknown"}")
        actor.sendMessage("")
        actor.sendHeader("Server information")
    }


}