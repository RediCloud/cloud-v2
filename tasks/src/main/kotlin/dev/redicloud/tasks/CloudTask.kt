package dev.redicloud.tasks

import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.tasks.executor.CloudTaskExecutor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.logging.Level

abstract class CloudTask() {

    companion object {
        private val LOGGER = LogManager.logger(CloudTask::class)
    }

    val id: UUID = UUID.randomUUID()
    private var canceled = false
    private var executors: MutableList<CloudTaskExecutor> = mutableListOf()
    private var executeCount: Int = 0
    private var started = false
    private lateinit var taskManager: CloudTaskManager

    abstract suspend fun execute(): Boolean

    @OptIn(DelicateCoroutinesApi::class)
    internal fun preExecute(source: CloudTaskExecutor) {
        if (canceled) return
        try {
            GlobalScope.launch {
                LOGGER.log(Level.FINEST, "Cloud task (${this::class.simpleName}) execute by ${source::class.simpleName}")
                if (execute()) {
                    cancel()
                }
            }
        }catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Error while executing cloud task (${this::class.simpleName}) by ${source::class.simpleName}", e)
        }
        executeCount++
    }

    fun start(manager: CloudTaskManager) {
        taskManager = manager
        if (canceled) return
        started = true
        executors.forEach { it.run(manager) }
    }

    fun getEventManager(): EventManager = taskManager.eventManager

    fun getPacketManager(): PacketManager = taskManager.packetManager

    fun getExecutors(): List<CloudTaskExecutor> = executors.toList()

    fun isCanceled(): Boolean = canceled
    fun isStarted(): Boolean = started

    fun cancel() {
        canceled = true
        executors.forEach { it.cancel() }
        LOGGER.log(Level.FINEST, "Canceled task ${this::class.simpleName}")
    }

}