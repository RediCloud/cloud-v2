package dev.redicloud.api.tasks

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.packets.AbstractPacket
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Builder for configuring execution triggers on a [ICloudTask].
 *
 * Supports multiple trigger types that can be combined:
 * - [instant] -- execute immediately
 * - [delay] -- execute after a delay
 * - [atTime] -- execute at a specific timestamp
 * - [period] -- execute periodically
 * - [event] -- execute when a [CloudEvent] fires
 * - [packet] -- execute when a packet is received
 *
 * Call [register] to finalize and start the task.
 */
interface ICloudTaskExecutorBuilder {

    /**
     * Sets the task to be executed.
     *
     * @param task the task instance
     * @return this builder
     */
    fun task(task: ICloudTask): ICloudTaskExecutorBuilder

    /**
     * Adds an instant executor that runs the task immediately upon registration.
     *
     * @return this builder
     */
    fun instant(): ICloudTaskExecutorBuilder

    /**
     * Adds an executor that runs the task at a specific timestamp.
     *
     * @param atTime the Unix timestamp (milliseconds) at which to execute
     * @return this builder
     * @throws IllegalArgumentException if [atTime] is in the past
     */
    fun atTime(atTime: Long): ICloudTaskExecutorBuilder

    /**
     * Adds an executor that runs the task after a delay.
     *
     * @param delay the delay in milliseconds
     * @return this builder
     */
    fun delay(delay: Long): ICloudTaskExecutorBuilder

    /**
     * Adds an executor that runs the task after a delay.
     *
     * @param duration the delay duration
     * @return this builder
     */
    fun delay(duration: Duration): ICloudTaskExecutorBuilder

    /**
     * Adds a periodic executor that runs the task at a fixed interval.
     *
     * @param period the interval between executions
     * @param maxExecutions the maximum number of executions, or `-1` for unlimited
     * @return this builder
     */
    fun period(period: Duration, maxExecutions: Int = -1): ICloudTaskExecutorBuilder

    /**
     * Adds an event-based executor that runs the task when the specified event fires.
     *
     * @param eventClazz the event class to listen for
     * @return this builder
     */
    fun event(eventClazz: KClass<out CloudEvent>): ICloudTaskExecutorBuilder

    /**
     * Adds a packet-based executor that runs the task when the specified packet is received.
     *
     * @param packetClazz the packet class to listen for
     * @return this builder
     */
    fun packet(packetClazz: KClass<out AbstractPacket>): ICloudTaskExecutorBuilder

    /**
     * Registers the task with all configured executors and starts execution.
     *
     * @return the registered task
     * @throws IllegalStateException if no task or executors have been set
     */
    fun register(): ICloudTask
}
