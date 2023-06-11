package dev.redicloud.service.node.tasks.file

import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.packets.FileTransferChunkPacket
import dev.redicloud.service.node.packets.FileTransferStartPacket
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.TEMP_FILE_TRANSFER_FOLDER
import dev.redicloud.utils.toCloudFile
import dev.redicloud.utils.unzipFile
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class FileReadTransferTask : CloudTask() {

    private val logger = LogManager.logger(this)
    private val timeout = 5.minutes.inWholeMilliseconds

    companion object {
        val RECEIVED_CHUNKS = mutableMapOf<UUID, MutableList<FileTransferChunkPacket>>()
        val RECEIVED_STARTS = mutableMapOf<UUID, FileTransferStartPacket>()
    }

    override suspend fun execute(): Boolean {
        val timeOuted = RECEIVED_STARTS.filter { it.value.receiveTime!!+timeout < System.currentTimeMillis() }
        timeOuted.forEach { RECEIVED_STARTS.remove(it.key) }

        val canceled = RECEIVED_CHUNKS.filter { !RECEIVED_STARTS.containsKey(it.key) }
        canceled.forEach { RECEIVED_CHUNKS.remove(it.key) }

        RECEIVED_STARTS.map { it.value }.forEach { startPacket ->
            val indexSize = startPacket.index
            val received = RECEIVED_CHUNKS
            val length = startPacket.chunkSize
            if (indexSize != (RECEIVED_CHUNKS[startPacket.transferId]?.count { it.transferId == startPacket.transferId } ?: 0)) return@forEach
            val folder = File(TEMP_FILE_TRANSFER_FOLDER.getFile().absolutePath, startPacket.transferId.toString())
            folder.mkdirs()
            val zip = File(folder.absolutePath, "data.zip")

            try {
                val outputStream = FileOutputStream(zip)
                for (id in 1 until indexSize) {
                    val packet = RECEIVED_CHUNKS[startPacket.transferId]!!.firstOrNull { it.chunkIndex == id }!!
                    outputStream.write(packet.data)
                }
                outputStream.close()

                val targetFile = toCloudFile(startPacket.cloudPath)
                if (targetFile.isDirectory) {
                    targetFile.deleteRecursively()
                }else {
                    targetFile.delete()
                }

                unzipFile(zip.absolutePath, targetFile.absolutePath)
            }catch (e: Exception) {
                logger.severe("Failed to write bytes of transfer ${startPacket.transferId}", e)
            }
            folder.delete()

            RECEIVED_CHUNKS.remove(startPacket.transferId)
            RECEIVED_STARTS.remove(startPacket.transferId)
        }
        return false
    }

}