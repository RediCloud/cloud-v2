package dev.redicloud.event

import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch

class InlineEventCaller<T>(val eventManager: EventManager, val handler: (T) -> Unit) {

    fun unregister() {
        defaultScope.launch { eventManager.unregister(this@InlineEventCaller) }
    }

    @CloudEventListener
    fun listener(event: T) = handler(event)
}