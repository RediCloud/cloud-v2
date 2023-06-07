package dev.redicloud.tasks.executor

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventManager
import dev.redicloud.event.InlineEventCaller
import dev.redicloud.tasks.CloudTask
import kotlin.reflect.KClass

class EventBasedCloudExecutor(
    task: CloudTask,
    val eventManager: EventManager,
    val events: List<KClass<out CloudEvent>>
) : CloudTaskExecutor(task) {

    private val listeners = mutableListOf<InlineEventCaller<*>>()

    override suspend fun run() {
        events.forEach { listener(it) }
        onFinished {
            listeners.forEach {
                eventManager.unregister(it)
            }
        }
    }

    private fun listener(clazz: KClass<out CloudEvent>) {
        val listener = eventManager.listen(clazz) {
            cloudTask.preExecute()
        }
        listeners.add(listener)
    }

}