package dev.redicloud.service.node.repository.template.file

import dev.redicloud.logging.LogManager
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
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration.Companion.seconds

internal var WATCH_SERVICE: WatchService? = null
private var watcherScope: CoroutineScope? = null
private var fileRequestListener: PacketListener<FileTransferRequestPacket>? = null
val FILE_WATCHER_LOCK = ReentrantLock(true)
private val logger = LogManager.Companion.logger(FileTemplateRepository::class)

internal val REGISTERED_PATHS = mutableListOf<Path>()

fun FileTemplateRepository.connectFileWatcher() {
    val storage = STORAGE_FOLDER.getFile().toPath()
    STORAGE_FOLDER.createIfNotExists()
    TEMPLATE_FOLDER.createIfNotExists()

    WATCH_SERVICE = FileSystems.getDefault().newWatchService()
    watchDirectory(storage.toFile())
    val templateFolder = TEMPLATE_FOLDER.getFile()
    watchDirectory(templateFolder)

    @OptIn(DelicateCoroutinesApi::class)
    watcherScope = CoroutineScope(newSingleThreadContext("template-file-watcher"))
    watcherScope!!.launch {
        while (true) {
            try {
                val key = WATCH_SERVICE!!.take()
                val dir: Path = key.watchable() as Path
                val events = key.pollEvents()
                if (!FILE_WATCHER_LOCK.isLocked && events.isNotEmpty()) {
                    var folderCreated = false
                    events.forEach { event ->
                        val localCreated = folderCreated
                        folderCreated = false
                        val kind = event.kind()
                        val eventPath = event.context() as Path
                        val file = dir.resolve(eventPath).toFile()
                        if (!isInFile(templateFolder, file)) return@forEach
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY && file.isDirectory && !localCreated) {
                            return@forEach
                        }
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE && file.isDirectory) {
                            watchDirectory(file, true)
                            folderCreated = true
                        }
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            REGISTERED_PATHS.remove(eventPath)
                        }
                        publish(file, null, 0)
                    }
                }
                key.reset()
            }catch (e: Exception) {
                if (e is ClosedWatchServiceException) return@launch
                logger.severe("Error in file watcher!", e)
            }
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

internal fun FileTemplateRepository.watchDirectory(file: File, force: Boolean = false) {
    if (!file.exists() || file.isFile) return
    if (WATCH_SERVICE == null) return
    val path = file.toPath()
    if (!REGISTERED_PATHS.contains(path) || force) {
        path.register(
            WATCH_SERVICE!!,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
        REGISTERED_PATHS.add(path)
    }
    file.listFiles()?.forEach { watchDirectory(it, force) }
}

fun FileTemplateRepository.disconnectFileWatcher() {
    watcherScope?.cancel()
    WATCH_SERVICE?.close()
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