package dev.redicloud.service.node.listener

import dev.redicloud.api.packets.PacketListener
import dev.redicloud.console.Console
import dev.redicloud.service.node.packets.NodeConsoleCommandPacket

class NodeConsoleCommandListener(console: Console) : PacketListener<NodeConsoleCommandPacket>(
    NodeConsoleCommandPacket::class,
    { packet ->
        console.executeCommand(packet.command)
    }
)