package dev.redicloud.service.minecraft.listener

import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.events.internal.node.NodeConnectEvent
import dev.redicloud.api.events.internal.node.NodeDisconnectEvent
import dev.redicloud.api.events.internal.node.NodeMasterChangedEvent
import dev.redicloud.api.events.internal.node.NodeSuspendedEvent
import dev.redicloud.api.events.internal.server.CloudServerStateChangeEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.service.ICloudService
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.repository.server.CloudServer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor

abstract class AbstractCloudNotificationListeners(
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
                    translateIdentifierName(node),
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
                    translateIdentifierName(node),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.GREEN),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("connected to the cluster").color(NamedTextColor.WHITE),
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
                    translateIdentifierName(node),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.RED),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("disconnected from the cluster").color(NamedTextColor.WHITE),
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
                    translateIdentifierName(node),
                    Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("● ").color(NamedTextColor.YELLOW),
                    Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                    Component.text().content("new master").color(NamedTextColor.WHITE),
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
                            translateIdentifierName(server),
                            Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("● ").color(NamedTextColor.YELLOW),
                            Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("preparing").color(NamedTextColor.WHITE),
                            Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                        )
                    }
                }

                CloudServerState.RUNNING -> {
                    sendMessage("redicloud.server.state.running", "server ${server.name}") {
                        it.append(
                            translateIdentifierName(server),
                            Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("● ").color(NamedTextColor.GREEN),
                            Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("running").color(NamedTextColor.WHITE),
                            Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                        )
                    }
                }

                CloudServerState.STOPPING -> {
                    sendMessage("redicloud.server.state.stopping") {
                        it.append(
                            translateIdentifierName(server),
                            Component.text().content(": ").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("● ").color(NamedTextColor.RED),
                            Component.text().content("(").color(NamedTextColor.DARK_GRAY),
                            Component.text().content("stopping").color(NamedTextColor.WHITE),
                            Component.text().content(")").color(NamedTextColor.DARK_GRAY)
                        )
                    }
                }

                else -> return@runBlocking
            }
        }
    }

    private fun translateIdentifierName(service: ICloudService): TextComponent {
        return Component.text().append(
            Component.text().content(service.name).color(NamedTextColor.AQUA)
        ).build()
    }

    abstract fun sendMessage(permission: String, clickCommand: String? = null, lambda: (TextComponent.Builder) -> Unit)

}