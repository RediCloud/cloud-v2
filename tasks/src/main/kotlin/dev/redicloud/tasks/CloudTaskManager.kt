package dev.redicloud.tasks

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.tasks.ICloudTask
import dev.redicloud.api.tasks.ICloudTaskExecutorBuilder
import dev.redicloud.api.tasks.ICloudTaskManager
import dev.redicloud.logging.LogManager
import dev.redicloud.tasks.executor.AtTimeCloudExecutor
import dev.redicloud.tasks.executor.CloudTaskExecutor
import dev.redicloud.tasks.executor.EventBasedCloudExecutor
import dev.redicloud.tasks.executor.InstantCloudExecutor
import dev.redicloud.tasks.executor.PacketBasedCloudExecutor
import dev.redicloud.tasks.executor.PeriodicallyCloudTaskExecutor
import dev.redicloud.utils.coroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration

class CloudTaskManager(
    internal val eventManager: IEventManager,
    internal val packetManager: IPacketManager,
    threads: Int
) : ICloudTaskManager {

    companion object {
        val LOGGER = LogManager.logger(CloudTaskManager::class)
    }

    internal val tasks = ConcurrentHashMap<UUID, CloudTask>()

    @OptIn(DelicateCoroutinesApi::class)
    internal val scope = CoroutineScope(
        SupervisorJob() + newFixedThreadPoolContext(threads, "CloudTaskManager") + coroutineExceptionHandler
    )

    override fun register(task: ICloudTask): UUID {
        require(task is CloudTask) { "Task must be an instance of CloudTask" }
        require(!tasks.containsKey(task.id)) { "Task with id ${task.id} is already registered" }
        tasks[task.id] = task
        task.start(this)
        return task.id
    }

    override fun unregister(id: UUID): ICloudTask? {
        val task = tasks.remove(id)
        task?.cancel()
        return task
    }

    override fun unregister(task: ICloudTask): ICloudTask? = unregister(task.id)

    override fun builder(): ICloudTaskExecutorBuilder = CloudTaskExecutorBuilder(this)

    override fun getTasks(): List<ICloudTask> = tasks.values.toList()
}

class CloudTaskExecutorBuilder internal constructor(val manager: CloudTaskManager) : ICloudTaskExecutorBuilder {

    private var task: CloudTask? = null
    private var executors: MutableList<CloudTaskExecutor> = mutableListOf()
    private var events: MutableList<KClass<out CloudEvent>> = mutableListOf()
    private var packets: MutableList<KClass<out AbstractPacket>> = mutableListOf()
    private var instant: Boolean = false

    override fun task(task: ICloudTask): ICloudTaskExecutorBuilder {
        require(task is CloudTask) { "Task must be an instance of CloudTask" }
        this.task = task
        return this
    }

    override fun instant(): ICloudTaskExecutorBuilder {
        this.instant = true
        return this
    }

    override fun atTime(atTime: Long): ICloudTaskExecutorBuilder {
        require(atTime >= System.currentTimeMillis()) { "At time must be in the future" }
        executors.add(AtTimeCloudExecutor(this.task!!, atTime))
        return this
    }

    override fun delay(delay: Long): ICloudTaskExecutorBuilder {
        require(delay >= 0) { "Delay must be positive" }
        executors.add(AtTimeCloudExecutor(this.task!!, System.currentTimeMillis() + delay))
        return this
    }

    override fun delay(duration: Duration): ICloudTaskExecutorBuilder {
        require(duration.inWholeMilliseconds >= 0) { "Delay must be positive" }
        executors.add(AtTimeCloudExecutor(this.task!!, System.currentTimeMillis() + duration.inWholeMilliseconds))
        return this
    }

    override fun period(period: Duration, maxExecutions: Int): ICloudTaskExecutorBuilder {
        require(period.inWholeMilliseconds >= 0) { "Period must be positive" }
        executors.add(PeriodicallyCloudTaskExecutor(this.task!!, period, maxExecutions))
        return this
    }

    override fun event(eventClazz: KClass<out CloudEvent>): ICloudTaskExecutorBuilder {
        events.add(eventClazz)
        return this
    }

    override fun packet(packetClazz: KClass<out AbstractPacket>): ICloudTaskExecutorBuilder {
        packets.add(packetClazz)
        return this
    }

    override fun register(): ICloudTask {
        checkNotNull(this.task) { "Task must be set" }
        check(this.executors.isNotEmpty() || this.events.isNotEmpty() || this.packets.isNotEmpty() || instant) {
            "At least one executor must be set"
        }
        val task = this.task!!
        executors.add(EventBasedCloudExecutor(task, manager.eventManager, events))
        executors.add(PacketBasedCloudExecutor(task, manager.packetManager, packets))
        if (instant) executors.add(InstantCloudExecutor(task))
        executors.forEach { task.addExecutor(it) }
        manager.register(task)
        return task
    }
}
