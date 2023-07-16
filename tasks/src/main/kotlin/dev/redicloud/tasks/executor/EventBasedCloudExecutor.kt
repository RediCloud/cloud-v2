package dev.redicloud.tasks.executor

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventManager
import dev.redicloud.event.InlineEventCaller
import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class EventBasedCloudExecutor(
    task: CloudTask,
    val eventManager: EventManager,
    val events: List<KClass<out CloudEvent>>
) : CloudTaskExecutor(task) {

    private val listeners = mutableListOf<InlineEventCaller<*>>()

    override suspend fun run() {
        events.forEach { listener(it) }
        cloudTask.onFinished {
            listeners.forEach {
                eventManager.unregisterListener(it)
            }
        }
    }

    private fun listener(clazz: KClass<out CloudEvent>) {
        val listener = eventManager.listen(clazz) {
            cloudTask.taskManager.scope.launch {
                cloudTask.preExecute(this@EventBasedCloudExecutor)?.join()
            }
        }
        listeners.add(listener)
    }

}