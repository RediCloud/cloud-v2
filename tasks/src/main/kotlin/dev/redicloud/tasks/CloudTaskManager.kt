package dev.redicloud.tasks

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventManager
import dev.redicloud.packets.AbstractPacket
import dev.redicloud.packets.PacketManager
import dev.redicloud.tasks.executor.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration

class CloudTaskManager(val eventManager: EventManager, val packetManager: PacketManager) {

    private val tasks = ConcurrentHashMap<UUID, CloudTask>()

    @OptIn(DelicateCoroutinesApi::class)
    internal val scope = CoroutineScope(newFixedThreadPoolContext(2, "CloudTaskManager"))

    fun register(task: CloudTask): UUID {
        if (tasks.containsKey(task.id)) throw IllegalArgumentException("Task with id ${task.id} is already registered")
        tasks[task.id] = task
        task.start(this)
        return task.id
    }

    fun unregister(id: UUID): CloudTask? = tasks.remove(id)

    fun unregister(task: CloudTask): CloudTask? = unregister(task.id)

    fun builder(): CloudTaskExecutorBuilder = CloudTaskExecutorBuilder(this)

}

class CloudTaskExecutorBuilder internal constructor(val manager: CloudTaskManager) {

    private var task: CloudTask? = null
    private var executors: MutableList<CloudTaskExecutor> = mutableListOf()
    private var events: MutableList<KClass<out CloudEvent>> = mutableListOf()
    private var packets: MutableList<KClass<out AbstractPacket>> = mutableListOf()

    fun task(task: CloudTask): CloudTaskExecutorBuilder {
        this.task = task
        return this
    }

    fun atTime(atTime: Long): CloudTaskExecutorBuilder {
        if (atTime < System.currentTimeMillis()) throw IllegalArgumentException("At time must be in the future")
        executors.add(AtTimeCloudExecutor(this.task!!, atTime))
        return this
    }

    fun delay(delay: Long): CloudTaskExecutorBuilder {
        if (delay < 0) throw IllegalArgumentException("Delay must be positive")
        executors.add(AtTimeCloudExecutor(this.task!!, System.currentTimeMillis() + delay))
        return this
    }

    fun period(period: Duration, maxExecutions: Int = -1): CloudTaskExecutorBuilder {
        if (period.inWholeMilliseconds < 0) throw IllegalArgumentException("Period must be positive")
        executors.add(PeriodicallyCloudTaskExecutor(this.task!!, period, maxExecutions))
        return this
    }

    fun event(eventClazz: KClass<out CloudEvent>): CloudTaskExecutorBuilder {
        events.add(eventClazz)
        return this
    }

    fun packet(packetClazz: KClass<out AbstractPacket>): CloudTaskExecutorBuilder {
        packets.add(packetClazz)
        return this
    }

    fun register(): CloudTask {
        if (this.task == null) throw IllegalStateException("Task must be set")
        if (this.executors.isEmpty() && this.events.isEmpty() && this.packets.isEmpty()) {
            throw IllegalStateException("At least one executor must be set")
        }
        val task = this.task!!
        executors.add(EventBasedCloudExecutor(task, manager.eventManager, events))
        executors.add(PacketBasedCloudExecutor(task, manager.packetManager, packets))
        manager.register(task)
        return task
    }

}