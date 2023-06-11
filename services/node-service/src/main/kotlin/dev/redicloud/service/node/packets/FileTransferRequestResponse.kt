package dev.redicloud.service.node.packets

import dev.redicloud.packets.AbstractPacket
import java.util.UUID

class FileTransferRequestResponse(val transferId: UUID?) : AbstractPacket()