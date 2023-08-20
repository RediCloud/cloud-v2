package dev.redicloud.api.events

import kotlin.reflect.KClass

interface IEventManager {

    val identifier: String

    fun registerListener(listener: Any)

    fun registerListeners(vararg listeners: Any) {
        listeners.forEach { registerListener(it) }
    }

    fun registerInlineListener(listener: InlineEventCaller<*>)

    fun unregisterListener(listener: Any)

    fun fireEvent(event: CloudEvent)

}

inline fun <reified T : CloudEvent> IEventManager.listen(noinline handler: (T) -> Unit): InlineEventCaller<T> {
    val listener = InlineEventCaller(this, T::class, handler)
    registerInlineListener(listener)
    return listener
}

fun <T : CloudEvent> IEventManager.listen(clazz: KClass<T>, handler: (T) -> Unit): InlineEventCaller<T> {
    val listener = InlineEventCaller(this, clazz, handler)
    registerInlineListener(listener)
    return listener
}