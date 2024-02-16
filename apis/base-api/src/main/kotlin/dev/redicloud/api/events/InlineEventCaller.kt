package dev.redicloud.api.events

import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class InlineEventCaller<T : CloudEvent>(
    val eventManager: IEventManager,
    val eventClass: KClass<T>,
    val handler: (T) -> Unit
) {

    val classLoader = this::class.java.classLoader

    fun unregister() {
        defaultScope.launch { eventManager.unregisterListener(this@InlineEventCaller) }
    }

    @CloudEventListener
    fun listener(event: T) = handler(event)
}