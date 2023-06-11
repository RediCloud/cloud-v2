package dev.redicloud.service.node.packets

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.service.node.tasks.file.FileReadTransferTask
import java.util.*

class FileTransferChunkPacket(val transferId: UUID, val chunkIndex: Int, val data: ByteArray) : AbstractPacket() {

    //TODO CHECK
    override fun received() {
        FileReadTransferTask.RECEIVED_CHUNKS[transferId] =
            (FileReadTransferTask.RECEIVED_CHUNKS[transferId] ?: mutableListOf()).also {
                it.add(this)
            }
    }


}