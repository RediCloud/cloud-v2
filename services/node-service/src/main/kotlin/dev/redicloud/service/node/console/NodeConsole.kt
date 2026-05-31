package dev.redicloud.service.node.console

import dev.redicloud.api.events.internal.node.NodeConnectEvent
import dev.redicloud.api.events.internal.node.NodeDisconnectEvent
import dev.redicloud.api.events.internal.node.NodeMasterChangedEvent
import dev.redicloud.api.events.internal.node.NodeSuspendedEvent
import dev.redicloud.api.events.internal.server.CloudServerConnectedEvent
import dev.redicloud.api.events.internal.server.CloudServerDeleteEvent
import dev.redicloud.api.events.internal.server.CloudServerDisconnectedEvent
import dev.redicloud.api.events.internal.server.CloudServerStateChangeEvent
import dev.redicloud.api.events.internal.server.CloudServerTransferredEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.commands.api.PARSERS
import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen
import dev.redicloud.console.utils.ScreenParser
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.event.EventManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch

@Suppress("LongMethod")
class NodeConsole(
    nodeConfiguration: NodeConfiguration,
    eventManager: EventManager,
    nodeRepository: NodeRepository,
    serverRepository: ServerRepository
) : Console(nodeConfiguration.nodeName, eventManager, true) {

    init {
        eventManager.listen<NodeSuspendedEvent> {
            val node = nodeRepository.getNode(it.serviceId) ?: return@listen
            val suspender = nodeRepository.getNode(it.suspender)
            writeLine(
                "${node.identifyName()}§8: §4● §8(%tc%suspended by " +
                    "${suspender?.identifyName(false) ?: toConsoleValue("unknown")} " +
                    "because node is reachable§8)"
            )
        }

        eventManager.listen<NodeConnectEvent> {
            val node = nodeRepository.getNode(it.serviceId) ?: return@listen
            writeLine("${node.identifyName()}§8: §2● §8(%tc%connected to the cluster§8)")
        }

        eventManager.listen<NodeDisconnectEvent> {
            val node = nodeRepository.getNode(it.serviceId) ?: return@listen
            writeLine("${node.identifyName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
        }

        eventManager.listen<NodeMasterChangedEvent> {
            val node = nodeRepository.getNode(it.serviceId) ?: return@listen
            writeLine("${node.identifyName()}§8: §6● §8(%tc%new master§8)")
        }

        eventManager.listen<CloudServerConnectedEvent> {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@listen
            writeLine("${server.identifyName()}§8: §2● §8(%tc%connected to the cluster§8)")
        }

        eventManager.listen<CloudServerDisconnectedEvent> {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@listen
            writeLine("${server.identifyName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
        }

        eventManager.listen<CloudServerDeleteEvent> {
            writeLine("%hc%${it.name}§8#%tc%${it.serviceId.id}§8: §4● §8(%tc%deleted§8)")
        }

        eventManager.listen<CloudServerStateChangeEvent> {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@listen
            if (it.state == CloudServerState.PREPARING) {
                writeLine("${server.identifyName()}§8: §6● §8(%tc%preparing start§8)")
            }
        }

        eventManager.listen<CloudServerTransferredEvent> {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@listen
            val node = nodeRepository.getNode(server.hostNodeId) ?: return@listen
            writeLine("${server.identifyName()}§8: §5● §8(%tc%transferred to ${node.identifyName()}§8)")
        }

        this.sendHeader()
        PARSERS[Screen::class] = ScreenParser(this)
    }

    override fun handleUserInterrupt(e: Exception) {
        defaultScope.launch {
            commandManager.getCommand(
                "exit"
            )!!.getSubCommand("")!!.execute(commandManager.defaultActor, emptyList())
        }
    }
}
