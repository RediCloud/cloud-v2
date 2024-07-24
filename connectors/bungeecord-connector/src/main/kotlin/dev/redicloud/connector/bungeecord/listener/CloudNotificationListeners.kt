package dev.redicloud.connector.bungeecord.listener

import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.minecraft.listener.AbstractCloudNotificationListeners
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.ProxyServer

class CloudNotificationListeners(
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
        val bungeeCordComponent = BungeeComponentSerializer.get().serialize(builder.build())
        ProxyServer.getInstance().players.filter { it.hasPermission(permission) }.forEach {
            it.sendMessage(*bungeeCordComponent)
        }
    }

}