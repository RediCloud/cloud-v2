package dev.redicloud.api.events

import kotlin.reflect.KClass

class InlineEventCaller<T : CloudEvent>(
    val eventManager: IEventManager,
    val eventClass: KClass<T>,
    val handler: suspend (T) -> Unit
) {

    val classLoader = this::class.java.classLoader

    fun unregister() {
        eventManager.unregisterListener(this@InlineEventCaller)
    }

    @CloudEventListener
    suspend fun listener(event: T) = handler(event)
}
