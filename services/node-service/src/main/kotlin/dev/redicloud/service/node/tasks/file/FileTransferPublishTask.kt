package dev.redicloud.service.node.tasks.file

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.service.node.packets.FileDeletePacket
import dev.redicloud.service.node.packets.FileTransferChunkPacket
import dev.redicloud.service.node.packets.FileTransferStartPacket
import dev.redicloud.service.node.repository.template.file.FileTransferRequest
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.TEMP_FILE_TRANSFER_FOLDER
import dev.redicloud.utils.service.ServiceType
import dev.redicloud.utils.toUniversalPath
import dev.redicloud.utils.zipFile
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.math.ceil

class FileTransferPublishTask(
    databaseConnection: DatabaseConnection,
    private val nodeRepository: NodeRepository,
    private val packetManager: PacketManager
) : CloudTask() {

    companion object {
        val PUBLISH_QUEUE = LinkedList<FileTransferRequest>()
        private val chunkSize = 5242880L
        private val transferPacketDelay = 500L
    }

    private val lock = databaseConnection.getClient().getLock("file-transfer")

    override suspend fun execute(): Boolean {
        if (PUBLISH_QUEUE.isEmpty()) return false

        val toRemove = mutableListOf<FileTransferRequest>()
        PUBLISH_QUEUE.forEach {request ->
            if (PUBLISH_QUEUE.filter { it.requestTime > request.requestTime }.any { it.file == request.file } ) {
                toRemove.add(request)
            }
        }
        toRemove.forEach { PUBLISH_QUEUE.remove(it) }

        if (PUBLISH_QUEUE.isEmpty()) return false

        lock.lock()

        val request = PUBLISH_QUEUE.peek()
        val targetFile = request.file
        PUBLISH_QUEUE.remove(request)

        try {
            if (nodeRepository.getConnectedNodes().count { it.serviceId != nodeRepository.serviceId } == 0) {
                PUBLISH_QUEUE.clear()
                request.block(null)
                return false
            }

            val templateCloudPath = toUniversalPath(targetFile)

            if (!targetFile.exists()) {
                packetManager.publish(FileDeletePacket(toUniversalPath(targetFile)), ServiceType.NODE)
                request.block(null)
                return false
            }
            val transferId = UUID.randomUUID()
            request.block(transferId)

            val folder = File(TEMP_FILE_TRANSFER_FOLDER.getFile().absolutePath, transferId.toString())
            folder.mkdirs()

            val zip = File(folder.absolutePath, "data.zip")
            zipFile(targetFile.absolutePath, zip.absolutePath)

            val fileSize = zip.length()
            val index = ceil(fileSize.toDouble() / chunkSize).toInt()
            val packets = mutableListOf<FileTransferChunkPacket>()

            val inputStream = FileInputStream(zip)
            var bytesRead = 0L
            var currentChunkSize = 0
            var id = 0
            for (chunkIndex in 1..index) {
                val data = ByteArray(fileSize.coerceAtMost((fileSize - bytesRead)).toInt())
                val bytesReadThisChunk = inputStream.read(data)
                bytesRead += bytesReadThisChunk
                packets.add(FileTransferChunkPacket(transferId, chunkIndex, data))
                id++
            }
            inputStream.close()

            if (nodeRepository.getConnectedNodes().count { it.serviceId != nodeRepository.serviceId } == 0) {
                PUBLISH_QUEUE.clear()
                return false
            }

            val startPacket = FileTransferStartPacket(packets.size, chunkSize, transferId, templateCloudPath, fileSize, targetFile.isDirectory, request.deleteTargetBeforeUnzip)
            if (request.singleTarget != null) {
                packetManager.publish(startPacket, request.singleTarget)
            }else {
                packetManager.publish(startPacket, ServiceType.NODE)
            }
            packets.forEach {
                if (request.singleTarget != null) {
                    packetManager.publish(it, request.singleTarget)
                }else {
                    packetManager.publish(it, ServiceType.NODE)
                }
                delay(transferPacketDelay)
            }
            folder.delete()
        } finally {
            lock.unlock()
        }
        return false
    }
}