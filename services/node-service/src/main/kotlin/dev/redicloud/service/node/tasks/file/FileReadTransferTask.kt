package dev.redicloud.service.node.tasks.file

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.template.file.FileTemplateRepository
import dev.redicloud.service.node.packets.FileTransferChunkPacket
import dev.redicloud.service.node.packets.FileTransferStartPacket
import dev.redicloud.service.node.repository.template.file.FILE_WATCHER_LOCK
import dev.redicloud.service.node.repository.template.file.watchDirectory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.TEMP_FILE_TRANSFER_FOLDER
import dev.redicloud.utils.toCloudFile
import dev.redicloud.utils.unzipFile
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.time.Duration.Companion.minutes

class FileReadTransferTask(val fileTemplateRepository: FileTemplateRepository) : CloudTask() {

    private val logger = LogManager.logger(this)
    private val timeout = 5.minutes.inWholeMilliseconds

    companion object {
        val RECEIVED_CHUNKS = mutableMapOf<UUID, MutableList<FileTransferChunkPacket>>()
        val RECEIVED_STARTS = mutableMapOf<UUID, FileTransferStartPacket>()
    }

    override suspend fun execute(): Boolean {
        val timeOuted = RECEIVED_STARTS.filter { it.value.receiveTime!! + timeout < System.currentTimeMillis() }
        timeOuted.forEach { RECEIVED_STARTS.remove(it.key) }

        val canceled = RECEIVED_CHUNKS.filter { !RECEIVED_STARTS.containsKey(it.key) }
        canceled.forEach { RECEIVED_CHUNKS.remove(it.key) }

        RECEIVED_STARTS.map { it.value }.forEach { startPacket ->
            val indexSize = startPacket.index
            val received = RECEIVED_CHUNKS
            val length = startPacket.chunkSize
            if (indexSize != (RECEIVED_CHUNKS[startPacket.transferId]?.count { it.transferId == startPacket.transferId }
                    ?: 0)) return@forEach
            val folder = File(TEMP_FILE_TRANSFER_FOLDER.getFile().absolutePath, startPacket.transferId.toString())
            folder.mkdirs()
            val zip = File(folder.absolutePath, "data.zip")

            FILE_WATCHER_LOCK.lock()
            try {
                val outputStream = FileOutputStream(zip)
                for (id in 0 until indexSize) {
                    val packet = RECEIVED_CHUNKS[startPacket.transferId]!!.firstOrNull { it.chunkIndex == id + 1 }!!
                    outputStream.write(packet.data)
                }
                outputStream.close()

                val realTarget = toCloudFile(startPacket.cloudPath)
                if (!realTarget.exists() && startPacket.isFolder) {
                    realTarget.mkdirs()
                }else if(!realTarget.parentFile.exists()) {
                    realTarget.parentFile.mkdirs()
                }
                val zipSize = zip.length()
                val expectedSize = startPacket.fileSize
                if (zipSize != expectedSize) {
                    logger.severe("Failed to transfer file ${startPacket.cloudPath} because the file size is not equal to the expected size (${zip.length()} != ${startPacket.fileSize})")
                } else {
                    if (startPacket.deleteTargetBeforeUnzip && realTarget.exists()) {
                        if (realTarget.isFile) {
                            realTarget.delete()
                        }else {
                            realTarget.deleteRecursively()
                        }
                    }
                    unzipFile(zip.absolutePath, realTarget.parentFile.absolutePath)
                    fileTemplateRepository.watchDirectory(realTarget.parentFile)
                }
            } catch (e: Exception) {
                logger.severe("Failed to write bytes of transfer ${startPacket.transferId}", e)
            } finally {
                FILE_WATCHER_LOCK.unlock()
            }
            folder.delete()

            RECEIVED_CHUNKS.remove(startPacket.transferId)
            RECEIVED_STARTS.remove(startPacket.transferId)
        }
        return false
    }

}