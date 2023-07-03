package dev.redicloud.service.node.console

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.api.server.events.server.CloudServerConnectedEvent
import dev.redicloud.console.Console
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.event.EventManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.service.base.events.node.NodeConnectEvent
import dev.redicloud.service.base.events.node.NodeDisconnectEvent
import dev.redicloud.service.base.events.node.NodeMasterChangedEvent
import dev.redicloud.service.base.events.node.NodeSuspendedEvent
import dev.redicloud.service.base.events.server.CloudServerDisconnectedEvent
import dev.redicloud.service.base.events.server.CloudServerStateChangeEvent
import kotlinx.coroutines.runBlocking

class NodeConsole(
    nodeConfiguration: NodeConfiguration,
    eventManager: EventManager,
    nodeRepository: NodeRepository,
    serverRepository: ServerRepository
) : Console(nodeConfiguration.nodeName, eventManager, true) {

    private val onSuspendNode = eventManager.listen<NodeSuspendedEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            val suspender = nodeRepository.getNode(it.suspender)
            writeLine("${node.getIdentifyingName()}§8: §4● §8(%tc%suspended by ${suspender?.getIdentifyingName(false) ?: toConsoleValue("unknown")} because node is reachable§8)")
        }
    }

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §2● §8(%tc%connected to the cluster§8)")
        }
    }

    private val onNodeDisconnect = eventManager.listen<NodeDisconnectEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
        }
    }

    private val onNodeMasterChange = eventManager.listen<NodeMasterChangedEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §6● §8(%tc%new master§8)")
        }
    }

    private val onServerConnectedEvent = eventManager.listen<CloudServerConnectedEvent> {
        runBlocking {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
            writeLine("${server.getIdentifyingName()}§8: §2● §8(%tc%connected to the cluster§8)")
        }
    }

    private val onServerDisconnectedEvent = eventManager.listen<CloudServerDisconnectedEvent> {
        runBlocking {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
            writeLine("${server.getIdentifyingName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
        }
    }

    private val onServerStateChangeEvent = eventManager.listen<CloudServerStateChangeEvent> {
        runBlocking {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
            if (it.state == CloudServerState.PREPARING) {
                writeLine("${server.getIdentifyingName()}§8: §e● §8(%tc%starting§8)")
            }
        }
    }

    init {
        this.sendHeader()
    }

    override fun handleUserInterrupt(e: Exception) {
        commandManager.getCommand("exit")!!.getSubCommand("")!!.execute(commandManager.actor, emptyList())
    }

}