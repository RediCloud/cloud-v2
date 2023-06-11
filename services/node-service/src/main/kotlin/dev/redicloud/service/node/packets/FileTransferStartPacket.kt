package dev.redicloud.service.node.packets

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.service.node.tasks.file.FileReadTransferTask
import java.util.UUID

class FileTransferStartPacket(
    val index: Int,
    val chunkSize: Long,
    val transferId: UUID,
    val cloudPath: String
) : AbstractPacket() {

    var receiveTime: Long? = null
    override fun received() {
        receiveTime = System.currentTimeMillis()
        FileReadTransferTask.RECEIVED_STARTS[transferId] = this
    }

}