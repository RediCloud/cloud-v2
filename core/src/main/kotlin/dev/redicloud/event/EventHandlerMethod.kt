package dev.redicloud.event

import kotlin.reflect.KFunction

/**
 * Represents a single event handler binding: a listener instance, the annotated
 * function to invoke, and its dispatch priority.
 *
 * @property listener the object instance containing the handler function
 * @property function the Kotlin function reference to call when the event fires
 * @property priority the handler priority (higher values are invoked first)
 */
data class EventHandlerMethod(
    val listener: Any,
    val function: KFunction<*>,
    val priority: Int
)
