package dev.redicloud.service.node.repository.template.file

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
import java.nio.file.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration.Companion.seconds

private var watchService: WatchService? = null
private var watcherScope: CoroutineScope? = null
private var fileRequestListener: PacketListener<FileTransferRequestPacket>? = null
val FILE_WATCHER_LOCK = ReentrantLock(true)

fun FileTemplateRepository.connectFileWatcher() {
    val storage = STORAGE_FOLDER.getFile().toPath()
    STORAGE_FOLDER.createIfNotExists()
    TEMPLATE_FOLDER.createIfNotExists()

    watchService = FileSystems.getDefault().newWatchService()
    storage.register(
        watchService!!,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    )
    val templateFolder = TEMPLATE_FOLDER.getFile()

    @OptIn(DelicateCoroutinesApi::class)
    watcherScope = CoroutineScope(newSingleThreadContext("template-file-watcher"))
    watcherScope!!.launch {
        while (true) {
            val key = watchService!!.take()
            val events = key.pollEvents()
            if (!FILE_WATCHER_LOCK.isLocked) {
                events.forEach { event ->
                    val eventPath = event.context() as Path
                    val file = Paths.get(storage.toUri()).resolve(eventPath).toFile()
                    if (!isInFile(templateFolder, file)) return@forEach
                    publish(file, null, 0)
                }
            }
            key.reset()
        }
    }
    fileRequestListener = packetManager.listen<FileTransferRequestPacket> { request ->
        publish(
            toCloudFile(request.cloudPath),
            request.sender!!,
            request.priority,
            request.deleteTargetBeforeUnzip
        ) { transferId ->
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

fun FileTemplateRepository.publish(
    targetFile: File,
    singleTarget: ServiceId? = null,
    priority: Int,
    deleteTargetBeforeUnzip: Boolean = false,
    block: (UUID?) -> Unit = {}
) {
    if (singleTarget != null && singleTarget.type != ServiceType.NODE)
        throw IllegalArgumentException("Cant publish file to non-node-service!")
    FileTransferPublishTask.PUBLISH_QUEUE.add(
        FileTransferRequest(
            targetFile,
            singleTarget,
            priority,
            deleteTargetBeforeUnzip = deleteTargetBeforeUnzip,
            block = block
        )
    )
}

suspend fun FileTemplateRepository.request(
    targetFile: File,
    targetNode: ServiceId? = null,
    priority: Int = 0,
    deleteTargetBeforeUnzip: Boolean = false
): Pair<UUID?, ServiceId> {
    if (targetNode != null && targetNode.type != ServiceType.NODE)
        throw IllegalArgumentException("Cant request file from non-node-service!")
    val master = (targetNode ?: nodeRepository.getMasterNode()?.serviceId)
        ?: throw NullPointerException("There is no master node and no target node provided!")
    if (master == nodeRepository.serviceId)
        throw IllegalArgumentException("Cant request file from own file system!")
    val response = packetManager.publish(
        FileTransferRequestPacket(toUniversalPath(targetFile), priority, deleteTargetBeforeUnzip),
        master
    )
        .withTimeOut(15.seconds)
        .waitBlocking()
    if (response !is FileTransferRequestResponse) return null to master
    return response.transferId to response.sender!!
}

suspend fun FileTemplateRepository.pullTemplates(priority: Int = 0): Pair<UUID?, ServiceId>? {
    val master = nodeRepository.getMasterNode() ?: return null
    if (master.serviceId == nodeRepository.serviceId) return null
    return request(TEMPLATE_FOLDER.getFile(), priority = priority, deleteTargetBeforeUnzip = true)
}

data class FileTransferRequest(
    val file: File,
    val singleTarget: ServiceId? = null,
    val priority: Int = 0,
    val requestTime: Long = System.currentTimeMillis(),
    val deleteTargetBeforeUnzip: Boolean = false,
    val block: (UUID?) -> Unit = {},
)