package dev.redicloud.service.node.packets

import dev.redicloud.packets.AbstractPacket

class FileTransferRequestPacket(val cloudPath: String, val priority: Int) : AbstractPacket()