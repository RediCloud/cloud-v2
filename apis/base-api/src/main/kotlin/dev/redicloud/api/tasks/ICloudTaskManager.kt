package dev.redicloud.api.tasks

import java.util.UUID

/**
 * Manages the lifecycle of [ICloudTask] instances.
 *
 * Provides registration, unregistration, and a builder API for configuring
 * task execution triggers. Obtain an instance via Guice injection.
 */
interface ICloudTaskManager {

    /**
     * Registers and starts a task.
     *
     * @param task the task to register
     * @return the unique ID of the registered task
     * @throws IllegalArgumentException if a task with the same ID is already registered
     */
    fun register(task: ICloudTask): UUID

    /**
     * Unregisters and cancels a task by its ID.
     *
     * @param id the unique ID of the task
     * @return the unregistered task, or `null` if not found
     */
    fun unregister(id: UUID): ICloudTask?

    /**
     * Unregisters and cancels a task.
     *
     * @param task the task to unregister
     * @return the unregistered task, or `null` if not found
     */
    fun unregister(task: ICloudTask): ICloudTask?

    /** Returns all currently registered tasks. */
    fun getTasks(): List<ICloudTask>

    /** Creates a new builder for configuring and registering a task with executors. */
    fun builder(): ICloudTaskExecutorBuilder
}
