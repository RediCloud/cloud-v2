package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.api.commands.BooleanSuggester
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.service.base.suggester.CloudServerSuggester
import dev.redicloud.service.base.suggester.ConfigurationTemplateSuggester
import dev.redicloud.service.base.suggester.RegisteredCloudNodeSuggester
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Command("server")
@CommandAlias(["ser", "s", "servers"])
@CommandDescription("Manage the servers")
class ServerCommand(
    private val serverFactory: ServerFactory,
    private val serverRepository: ServerRepository,
    private val nodeRepository: NodeRepository
) : ICommand {

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
            actor.sendMessage("§8- %hc%${it.name}§8: ${it.state.color}● §8(%tc%${it.state.displayName}§8)")
        }
        actor.sendMessage("")
        actor.sendHeader("Registered servers")
    }

    @CommandSubPath("start <template> [count]")
    @CommandDescription("Queue a amount of servers with a configuration template")
    fun start(
        actor: ConsoleActor,
        @CommandParameter("template", true, ConfigurationTemplateSuggester::class) template: ConfigurationTemplate,
        @CommandParameter("count", false, IntegerSuggester::class) count: Int?
    ) = defaultScope.launch {
        actor.sendMessage("Queued ${toConsoleValue(count ?: 1)} server with template ${toConsoleValue(template.name)}...")
        serverFactory.queueStart(template, count ?: 1)
    }

    @CommandSubPath("startstatic <name> <id>")
    @CommandDescription("Queue a registered static server with the given name and id")
    fun startStatic(
        actor: ConsoleActor,
        @CommandParameter("name", true, ConfigurationTemplateSuggester::class) name: String,
        @CommandParameter("id", true, IntegerSuggester::class) id: Int
    ) = defaultScope.launch {
        val server = serverRepository.getRegisteredServers().firstOrNull {
            it.configurationTemplate.name.lowercase() == name.lowercase() && it.id == id
        }
        if (server == null) {
            actor.sendMessage("§cThere is no registered static server with name ${toConsoleValue(name, false)} and id ${toConsoleValue(id, false)}!")
            return@launch
        }
        if (!server.configurationTemplate.static) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.identifyName(false), false)} is not a static server!")
            return@launch
        }
        if (server.state == CloudServerState.STARTING || server.state == CloudServerState.PREPARING) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.identifyName(false), false)} is already starting!")
            return@launch
        }
        if (server.state == CloudServerState.RUNNING || server.state == CloudServerState.STOPPING) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.identifyName(false), false)} is already running!")
            return@launch
        }
        actor.sendMessage("Queued static server ${server.identifyName()}...")
        serverFactory.queueStart(server.serviceId)
    }

    @CommandSubPath("delete <server>")
    @CommandDescription("Delete a server")
    fun delete(
        actor: ConsoleActor,
        @CommandParameter("server", true, CloudServerSuggester::class) server: CloudServer
    ) = runBlocking {
        if (server.state != CloudServerState.STOPPED) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.name, false)} is not stopped!")
            return@runBlocking
        }
        actor.sendMessage("Queued deletion of server ${server.identifyName()}...")
        actor.sendMessage("Note: If the hosted node is not connected to the cluster, the server will be deleted when the node connects to the cluster!")
        serverFactory.queueDelete(server.serviceId)
    }

    @CommandSubPath("unregister <server>")
    @CommandDescription("Unregister a server (this will not delete the server files of a static server)")
    fun unregister(
        actor: ConsoleActor,
        @CommandParameter("server", true, CloudServerSuggester::class) server: CloudServer
    ) = runBlocking {
        if (server.state != CloudServerState.STOPPED) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.name, false)} is not stopped!")
            return@runBlocking
        }
        actor.sendMessage("Queued unregistration of server ${server.identifyName()}...")
        serverFactory.queueUnregister(server.serviceId)
    }

    @CommandSubPath("transfer <server> <node>")
    @CommandDescription("Transfer a static server to another node")
    fun transfer(
        actor: ConsoleActor,
        @CommandParameter("server", true, CloudServerSuggester::class) server: CloudServer,
        @CommandParameter("node", true, RegisteredCloudNodeSuggester::class) node: CloudNode
    ) = runBlocking {
        if (server.state != CloudServerState.STOPPED) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.name, false)} is not stopped!")
            return@runBlocking
        }
        if (server.hostNodeId == node.serviceId) {
            actor.sendMessage("§cThe server ${toConsoleValue(server.name, false)} is already hosted on node ${toConsoleValue(node.name, false)}!")
            return@runBlocking
        }
        actor.sendMessage("Queued transfer of static server ${server.identifyName()} to node ${toConsoleValue(node.name, false)}...")
        actor.sendMessage("Note: If the hosted node is not connected to the cluster, the server will be transferred when the node connects to the cluster!")
        serverFactory.queueTransfer(server.serviceId, node.serviceId)
    }

    @CommandSubPath("stop <server> [force]")
    @CommandDescription("Stop a server")
    fun stop(
        actor: ConsoleActor,
        @CommandParameter("server", true, CloudServerSuggester::class) server: String,
        @CommandParameter("force", false, BooleanSuggester::class) force: Boolean?
    ) = defaultScope.launch {
        val forceStop = force ?: false
        when {
            server == "*" -> stopAllServers(actor, forceStop)
            server.endsWith("*") -> stopServersByPrefix(actor, server.dropLast(1), forceStop)
            server.contains(",") -> stopServersByNames(actor, server.split(",").map { it.trim() }, forceStop)
            else -> stopServerByName(actor, server, forceStop)
        }
    }

    private suspend fun collectStoppableServers(forceStop: Boolean): List<CloudServer> {
        return serverRepository.getRegisteredServers().filter { server ->
            !(server.state == CloudServerState.STOPPING && !forceStop || server.state == CloudServerState.STOPPED)
        }
    }

    private suspend fun stopAllServers(actor: ConsoleActor, forceStop: Boolean) {
        val servers = collectStoppableServers(forceStop)
        if (servers.isEmpty()) {
            actor.sendMessage("No servers are connected!")
            return
        }
        actor.sendMessage("Stopping all servers...")
        servers.forEach { serverFactory.queueStop(it.serviceId, forceStop) }
    }

    private suspend fun stopServersByPrefix(actor: ConsoleActor, prefix: String, forceStop: Boolean) {
        val servers = collectStoppableServers(forceStop)
            .filter { it.name.lowercase().startsWith(prefix.lowercase()) }
        if (servers.isEmpty()) {
            actor.sendMessage("No server starts with ${toConsoleValue(prefix)} is connected!")
            return
        }
        actor.sendMessage("Stopping all servers starting with ${toConsoleValue(prefix)}...")
        servers.forEach { serverFactory.queueStop(it.serviceId, forceStop) }
    }

    private suspend fun stopServersByNames(actor: ConsoleActor, names: List<String>, forceStop: Boolean) {
        val servers = collectStoppableServers(forceStop)
            .filter { server -> names.any { it.lowercase() == server.name.lowercase() } }
        actor.sendMessage("Stopping ${toConsoleValue(servers.size)} servers...")
        servers.forEach { serverFactory.queueStop(it.serviceId, forceStop) }
    }

    private suspend fun stopServerByName(actor: ConsoleActor, name: String, forceStop: Boolean) {
        val servers = collectStoppableServers(forceStop)
            .filter { it.name.lowercase() == name.lowercase() }
        if (servers.isEmpty()) {
            actor.sendMessage("No server with name ${toConsoleValue(name)} connected!")
            return
        }
        actor.sendMessage("Stopping server ${toConsoleValue(name)}...")
        servers.forEach { serverFactory.queueStop(it.serviceId, forceStop) }
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
        actor.sendMessage("§8- %tc%State§8: ${server.state.color}● §8(%tc%${server.state.displayName}§8)")
        actor.sendMessage("§8- %tc%Configuration template§8: %hc%${server.configurationTemplate.name}")
        val node = nodeRepository.getNode(server.hostNodeId)
        actor.sendMessage("§8- %tc%Node§8: %hc%${node?.name ?: "unknown"}")
        actor.sendMessage("§8- %tc%Port§8: %hc%${server.port}")
        actor.sendMessage("§8- %tc%Player§8: %hc%${server.connectedPlayers.size}§8/%hc%${server.maxPlayers}")
        actor.sendMessage("§8- %tc%Hidden§8: %hc%${server.hidden.toSymbol()}")
        actor.sendMessage("")
        actor.sendHeader("Server information")
    }


}