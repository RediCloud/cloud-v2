package dev.redicloud.connector.bungeecord.listener

import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.events.internal.node.NodeConnectEvent
import dev.redicloud.api.events.internal.node.NodeDisconnectEvent
import dev.redicloud.api.events.internal.node.NodeMasterChangedEvent
import dev.redicloud.api.events.internal.node.NodeSuspendedEvent
import dev.redicloud.api.events.internal.server.CloudServerConnectedEvent
import dev.redicloud.api.events.internal.server.CloudServerDisconnectedEvent
import dev.redicloud.api.events.internal.server.CloudServerStateChangeEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.repository.server.CloudServer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.ProxyServer

class CloudServerListener(
    private val serverRepository: ICloudServerRepository,
    private val nodeRepository: ICloudNodeRepository,
    eventManager: IEventManager
) {

    private val onSuspendNode = eventManager.listen<NodeSuspendedEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            val suspender = nodeRepository.getNode(it.suspender)
            sendMessage("redicloud.node.suspend") {
                it.append(
                    Component.text().content(node.identifyName()).color(NamedTextColor.AQUA),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.RED),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("%tc%suspended by ${suspender?.identifyName()}").color(NamedTextColor.RED),
                    Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            sendMessage("redicloud.node.connect") {
                it.append(
                    Component.text().content(node.identifyName()).color(NamedTextColor.AQUA),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.GREEN),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("%tc%connected to the cluster").color(NamedTextColor.GREEN),
                    Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }

    private val onNodeDisconnect = eventManager.listen<NodeDisconnectEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            sendMessage("redicloud.node.disconnect") {
                it.append(
                    Component.text().content(node.identifyName()).color(NamedTextColor.AQUA),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.RED),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("%tc%disconnected from the cluster").color(NamedTextColor.RED),
                    Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }

    private val onNodeMasterChange = eventManager.listen<NodeMasterChangedEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            sendMessage("redicloud.node.master.change") {
                it.append(
                    Component.text().content(node.identifyName()).color(NamedTextColor.AQUA),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.YELLOW),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("%tc%is now the master node").color(NamedTextColor.YELLOW),
                    Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }

    private val onServerConnectedEvent = eventManager.listen<CloudServerConnectedEvent> {
        runBlocking {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
            sendMessage("redicloud.server.state.started") {
                it.append(
                    Component.text().content(server.identifyName()).color(NamedTextColor.AQUA),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.GREEN),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("%tc%connected to the cluster").color(NamedTextColor.GREEN),
                    Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }

    private val onServerDisconnectedEvent = eventManager.listen<CloudServerDisconnectedEvent> {
        runBlocking {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
            sendMessage("redicloud.server.state.stopped") {
                it.append(
                    Component.text().content(server.identifyName()).color(NamedTextColor.AQUA),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.RED),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("%tc%disconnected from the cluster").color(NamedTextColor.RED),
                    Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                )

            }
        }
    }

    private val onServerStateChangeEvent = eventManager.listen<CloudServerStateChangeEvent> {
        runBlocking {
            val server = serverRepository.getServer<CloudServer>(it.serviceId) ?: return@runBlocking
            when (it.state) {
                CloudServerState.PREPARING -> {
                    sendMessage("redicloud.server.state.preparing") {
                        it.append(
                            Component.text().content(server.identifyName()).color(NamedTextColor.AQUA),
                            Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("● ").color(NamedTextColor.YELLOW),
                            Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("%tc%preparing").color(NamedTextColor.YELLOW),
                            Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                        )
                    }
                }

                CloudServerState.RUNNING -> {
                    sendMessage("redicloud.server.state.running") {
                        it.append(
                            Component.text().content(server.identifyName()).color(NamedTextColor.AQUA),
                            Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("● ").color(NamedTextColor.GREEN),
                            Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("%tc%running").color(NamedTextColor.GREEN),
                            Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                        )
                    }
                }

                CloudServerState.STOPPING -> {
                    sendMessage("redicloud.server.state.stopping") {
                        it.append(
                            Component.text().content(server.identifyName()).color(NamedTextColor.AQUA),
                            Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("● ").color(NamedTextColor.RED),
                            Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("%tc%stopping").color(NamedTextColor.RED),
                            Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                        )
                    }
                }

                else -> return@runBlocking
            }
        }
    }

    private fun sendMessage(permission: String, lambda: (TextComponent.Builder) -> Unit) {
        val builder = Component.text()
        lambda(builder)
        val bungeeCordComponent = BungeeComponentSerializer.get().serialize(builder.build())
        ProxyServer.getInstance().players.filter { it.hasPermission(permission) }.forEach {
            it.sendMessage(*bungeeCordComponent)
        }
    }

}