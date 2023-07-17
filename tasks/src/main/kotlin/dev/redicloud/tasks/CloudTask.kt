package dev.redicloud.tasks

import dev.redicloud.tasks.executor.CloudTaskExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level

abstract class CloudTask(private val useLock: Boolean = true) {

    val id: UUID = UUID.randomUUID()
    private var canceled = false
    private var executors: MutableList<CloudTaskExecutor> = mutableListOf()
    private var executeCount: Int = 0
    private var started = false
    lateinit var taskManager: CloudTaskManager
    private val finishListener = mutableListOf<() -> Unit>()
    private val lock = ReentrantLock()

    abstract suspend fun execute(): Boolean

    internal fun preExecute(source: CloudTaskExecutor): Job? {
        if (canceled) return null
        return taskManager.scope.launch {
            executeCount++
            if (canceled) return@launch
            if (useLock) {
                lock.lock()
            }
            try {
                CloudTaskManager.LOGGER.log(
                    Level.FINEST,
                    "Cloud task ${this@CloudTask::class.simpleName} execute by ${source::class.simpleName}"
                )
                if (runBlocking { execute() }) {
                    runBlocking { cancel() }
                }
            } catch (e: Exception) {
                CloudTaskManager.LOGGER.log(
                    Level.SEVERE,
                    "Error while executing cloud task (${this@CloudTask::class.simpleName}) by ${source::class.simpleName}",
                    e
                )
            } finally {
                if (useLock) {
                    lock.unlock()
                }
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