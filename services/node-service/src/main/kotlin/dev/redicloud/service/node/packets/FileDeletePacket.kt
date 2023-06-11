package dev.redicloud.service.node.packets

import dev.redicloud.packets.AbstractPacket
import java.io.File

class FileDeletePacket(val cloudPath: String) : AbstractPacket() {

    override fun received() {
        val file = File(cloudPath)
        if (!file.exists()) return
        if (file.isFile) {
            file.delete()
        }else {
            file.deleteRecursively()
        }
    }

}