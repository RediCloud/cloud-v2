package dev.redicloud.api.tasks

import java.util.UUID

/**
 * Represents a schedulable cloud task that can be registered with [ICloudTaskManager].
 *
 * Tasks define work to be executed on various triggers (instant, delayed, periodic,
 * event-based, or packet-based) via [ICloudTaskExecutorBuilder].
 */
interface ICloudTask {

    /** Unique identifier for this task. */
    val id: UUID

    /**
     * Executes the task logic.
     *
     * @return `true` if the task should be automatically canceled after execution,
     *         `false` to keep the task active for further executions.
     */
    suspend fun execute(): Boolean

    /** Returns whether this task has been canceled. */
    fun isCanceled(): Boolean

    /** Returns whether this task has been started by a [ICloudTaskManager]. */
    fun isStarted(): Boolean

    /** Cancels this task and all its executors. */
    fun cancel()

    /**
     * Registers a callback that is invoked when the task finishes.
     *
     * @param block the callback to invoke on finish
     */
    fun onFinished(block: () -> Unit)
}
