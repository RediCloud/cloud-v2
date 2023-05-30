package dev.redicloud.event

class InlineEventCaller<T>(val handler: (T) -> Unit) {
    @CloudEventListener
    fun listener(event: T) = handler(event)
}