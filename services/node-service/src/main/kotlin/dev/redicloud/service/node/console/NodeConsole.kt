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
import kotlinx.coroutines.runBlocking

class NodeConsole(
    nodeConfiguration: NodeConfiguration,
    eventManager: EventManager,
    nodeRepository: NodeRepository,
    serverRepository: ServerRepository
) : Console(nodeConfiguration.nodeName, eventManager, true) {

    init {
        eventManager.listen<NodeSuspendedEvent> {
            runBlocking {
                val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
                val suspender = nodeRepository.getNode(it.suspender)
                writeLine(
                    "${node.identifyName()}§8: §4● §8(%tc%suspended by " +
                        "${suspender?.identifyName(false) ?: toConsoleValue("unknown")} " +
                        "because node is reachable§8)"
                )
            }
        }

        eventManager.listen<NodeConnectEvent> {
            runBlocking {
                val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
                writeLine("${node.identifyName()}§8: §2● §8(%tc%connected to the cluster§8)")
            }
        }

        eventManager.listen<NodeDisconnectEvent> {
            runBlocking {
                val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
                writeLine("${node.identifyName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
            }
        }

        eventManager.listen<NodeMasterChangedEvent> {
            runBlocking {
                val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
                writeLine("${node.identifyName()}§8: §6● §8(%tc%new master§8)")
            }
        }

        eventManager.listen<CloudServerConnectedEvent> {
            runBlocking {
                val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
                writeLine("${server.identifyName()}§8: §2● §8(%tc%connected to the cluster§8)")
            }
        }

        eventManager.listen<CloudServerDisconnectedEvent> {
            runBlocking {
                val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
                writeLine("${server.identifyName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
            }
        }

        eventManager.listen<CloudServerDeleteEvent> {
            runBlocking {
                writeLine("%hc%${it.name}§8#%tc%${it.serviceId.id}§8: §4● §8(%tc%deleted§8)")
            }
        }

        eventManager.listen<CloudServerStateChangeEvent> {
            runBlocking {
                val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
                if (it.state == CloudServerState.PREPARING) {
                    writeLine("${server.identifyName()}§8: §6● §8(%tc%preparing start§8)")
                }
            }
        }

        eventManager.listen<CloudServerTransferredEvent> {
            runBlocking {
                val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
                val node = nodeRepository.getNode(server.hostNodeId) ?: return@runBlocking
                writeLine("${server.identifyName()}§8: §5● §8(%tc%transferred to ${node.identifyName()}§8)")
            }
        }

        this.sendHeader()
        PARSERS[Screen::class] = ScreenParser(this)
    }

    override fun handleUserInterrupt(e: Exception) {
        runBlocking {
            commandManager.getCommand(
                "exit"
            )!!.getSubCommand("")!!.execute(commandManager.defaultActor, emptyList())
        }
    }
}
