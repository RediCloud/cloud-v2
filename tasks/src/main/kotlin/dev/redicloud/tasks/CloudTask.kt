package dev.redicloud.tasks

import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.tasks.executor.CloudTaskExecutor
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.logging.Level

abstract class CloudTask() {


    val id: UUID = UUID.randomUUID()
    private var canceled = false
    private var executors: MutableList<CloudTaskExecutor> = mutableListOf()
    private var executeCount: Int = 0
    private var started = false
    private lateinit var taskManager: CloudTaskManager
    private val finishListener = mutableListOf<() -> Unit>()

    //TODO: optional one at time execution, check via locks

    abstract suspend fun execute(): Boolean

    internal fun preExecute(source: CloudTaskExecutor) {
        if (canceled) return
        defaultScope.launch {
            try {
                CloudTaskManager.LOGGER.log(Level.FINEST, "Cloud task ${this@CloudTask::class.simpleName} execute by ${source::class.simpleName}")
                if (execute()) {
                    cancel()
                }
            } catch (e: Exception) {
                CloudTaskManager.LOGGER.log(
                    Level.SEVERE,
                    "Error while executing cloud task (${this::class.simpleName}) by ${source::class.simpleName}",
                    e
                )
            }
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

    internal fun addExecutor(executor: CloudTaskExecutor) {
        executors.add(executor)
    }

    fun isCanceled(): Boolean = canceled
    fun isStarted(): Boolean = started

    fun cancel() {
        canceled = true
        executors.forEach { it.cancel() }
        finish()
        CloudTaskManager.LOGGER.log(Level.FINEST, "Canceled task ${this::class.simpleName}")
        if (taskManager.tasks.contains(this)) taskManager.tasks.remove(id)
    }

    internal fun finish() {
        finishListener.forEach { it() }
    }

    fun onFinished(block: () -> Unit) {
        finishListener.add(block)
    }

}