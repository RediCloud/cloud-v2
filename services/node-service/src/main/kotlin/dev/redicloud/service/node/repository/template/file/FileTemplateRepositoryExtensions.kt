package dev.redicloud.service.node.repository.template.file

import com.google.gson.annotations.Expose
import dev.redicloud.packets.PacketListener
import dev.redicloud.repository.template.file.FileTemplateRepository
import dev.redicloud.service.node.packets.FileTransferRequestPacket
import dev.redicloud.service.node.packets.FileTransferRequestResponse
import dev.redicloud.service.node.tasks.file.FileTransferPublishTask
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private var watchService: WatchService? = null
private var watcherScope: CoroutineScope? = null
private var fileRequestListener: PacketListener<FileTransferRequestPacket>? = null

fun FileTemplateRepository.connectFileWatcher() {
    watchService = FileSystems.getDefault().newWatchService()
    val templateFolder = TEMPLATE_FOLDER.getFile().toPath()
    TEMPLATE_FOLDER.createIfNotExists()
    templateFolder.register(
        watchService!!,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    )
    @OptIn(DelicateCoroutinesApi::class)
    watcherScope = CoroutineScope(newSingleThreadContext("template-file-watcher"))
    watcherScope!!.launch {
        while (true) {
            val key = watchService!!.take()
            val thisNode = nodeRepository.getMasterNode()
            if (thisNode != null && thisNode.serviceId != nodeRepository.serviceId) {
                key.pollEvents().forEach { event ->
                    val kind = event.kind()
                    val file = templateFolder.resolve(event.context() as Path).toFile()
                    publish(file, null, 0)
                }
            }
            key.reset()
        }
    }
    fileRequestListener = packetManager.listen<FileTransferRequestPacket> { request ->
        publish(toCloudFile(request.cloudPath), request.sender!!, request.priority) { transferId ->
            ioScope.launch {
                val packet = FileTransferRequestResponse(transferId).asAnswerOf(request)
                request.getManager()!!.publish(packet, request.sender!!)
            }
        }
    }
}

fun FileTemplateRepository.disconnectFileWatcher() {
    watcherScope?.cancel()
    watchService?.close()
    fileRequestListener?.let { packetManager.unregisterListener(it) }
}

fun FileTemplateRepository.publish(targetFile: File, singleTarget: ServiceId? = null, priority: Int, block: (UUID?) -> Unit = {}) {
    if (singleTarget != null && singleTarget.type != ServiceType.NODE)
        throw IllegalArgumentException("Cant publish file to non-node-service!")
    FileTransferPublishTask.PUBLISH_QUEUE.add(FileTransferRequest(targetFile, singleTarget, priority, block))
}

suspend fun FileTemplateRepository.request(targetFile: File, targetNode: ServiceId? = null, priority: Int = 0): Pair<UUID?, ServiceId> {
    if (targetNode != null && targetNode.type != ServiceType.NODE)
        throw IllegalArgumentException("Cant request file from non-node-service!")
    val master = (targetNode ?: nodeRepository.getMasterNode()?.serviceId)
        ?: throw NullPointerException("There is no master node and no target node provided!")
    if (master == nodeRepository.serviceId)
        throw IllegalArgumentException("Cant request file from own file system!")
    val response = packetManager.publish(FileTransferRequestPacket(toUniversalPath(targetFile), priority), master)
        .withTimeOut(15.seconds)
        .waitBlocking()
    if (response !is FileTransferRequestResponse) return null to master
    return response.transferId to response.sender!!
}

suspend fun FileTemplateRepository.pullTemplates(priority: Int = 0): Pair<UUID?, ServiceId>? {
    val master = nodeRepository.getMasterNode() ?: return null
    if (master.serviceId == nodeRepository.serviceId) return null
    return request(TEMPLATE_FOLDER.getFile(), priority = priority)
}

data class FileTransferRequest(
    val file: File,
    val singleTarget: ServiceId? = null,
    val priority: Int = 0,
    val block: (UUID?) -> Unit = {}
)