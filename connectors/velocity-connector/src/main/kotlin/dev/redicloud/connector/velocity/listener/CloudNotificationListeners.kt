package dev.redicloud.connector.velocity.listener

import com.velocitypowered.api.proxy.ProxyServer
import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.minecraft.listener.AbstractCloudNotificationListeners
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent

class CloudNotificationListeners(
    private val proxyServer: ProxyServer,
    serverRepository: ICloudServerRepository,
    nodeRepository: ICloudNodeRepository,
    eventManager: IEventManager
) : AbstractCloudNotificationListeners(serverRepository, nodeRepository, eventManager) {

    override fun sendMessage(permission: String, clickCommand: String?, lambda: (TextComponent.Builder) -> Unit) {
        val builder = Component.text()
        if (clickCommand != null) {
            builder.clickEvent(ClickEvent.runCommand(clickCommand))
        }
        lambda(builder)
        val component = builder.build()
        this.proxyServer.allPlayers.filter { it.hasPermission(permission) }.forEach {
            it.sendMessage(component)
        }
    }

}