package dev.redicloud.tasks

import dev.redicloud.api.tasks.ICloudTask
import dev.redicloud.tasks.executor.CloudTaskExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.logging.Level

abstract class CloudTask(private val useLock: Boolean = true) : ICloudTask {

    override val id: UUID = UUID.randomUUID()
    private var canceled = false
    private var executors: MutableList<CloudTaskExecutor> = mutableListOf()
    private var executeCount: Int = 0
    private var started = false
    lateinit var taskManager: CloudTaskManager
    private val finishListener = mutableListOf<() -> Unit>()
    private val mutex = Mutex()

    abstract override suspend fun execute(): Boolean

    internal fun preExecute(source: CloudTaskExecutor): Job? {
        if (canceled) return null
        return taskManager.scope.launch {
            executeCount++
            if (canceled) return@launch
            val block: suspend () -> Unit = {
                @Suppress("TooGenericExceptionCaught")
                try {
                    CloudTaskManager.LOGGER.log(
                        Level.FINEST,
                        "Cloud task ${this@CloudTask::class.simpleName} execute by ${source::class.simpleName}"
                    )
                    if (execute()) {
                        cancel()
                    }
                } catch (e: Exception) {
                    CloudTaskManager.LOGGER.log(
                        Level.SEVERE,
                        "Error while executing cloud task " +
                            "(${this@CloudTask::class.simpleName}) by ${source::class.simpleName}",
                        e
                    )
                }
            }
            if (useLock) {
                mutex.withLock { block() }
            } else {
                block()
            }
        }
    }

    fun start(manager: CloudTaskManager) {
        taskManager = manager
        if (canceled) return
        started = true
        executors.forEach { it.run(manager) }
    }

    fun getExecutors(): List<CloudTaskExecutor> = executors.toList()

    internal fun addExecutor(executor: CloudTaskExecutor) {
        executors.add(executor)
    }

    override fun isCanceled(): Boolean = canceled
    override fun isStarted(): Boolean = started

    override fun cancel() {
        canceled = true
        executors.forEach { it.cancel() }
        finish()
        CloudTaskManager.LOGGER.log(Level.FINEST, "Canceled task ${this::class.simpleName}")
        if (taskManager.tasks.contains(this)) taskManager.tasks.remove(id)
    }

    internal fun finish() {
        finishListener.forEach { it() }
    }

    override fun onFinished(block: () -> Unit) {
        finishListener.add(block)
    }
}
