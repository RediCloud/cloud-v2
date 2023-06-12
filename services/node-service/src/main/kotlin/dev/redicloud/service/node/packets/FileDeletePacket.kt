package dev.redicloud.service.node.packets

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.service.node.repository.template.file.FILE_WATCHER_LOCK
import java.io.File

class FileDeletePacket(val cloudPath: String) : AbstractPacket() {

    override fun received() {
        FILE_WATCHER_LOCK.lock()
        try {
            val file = File(cloudPath)
            if (!file.exists()) return
            if (file.isFile) {
                file.delete()
            }else {
                file.deleteRecursively()
            }
        }finally {
            FILE_WATCHER_LOCK.unlock()
        }
    }

}