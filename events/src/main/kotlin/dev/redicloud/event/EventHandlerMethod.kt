package dev.redicloud.event

import kotlin.reflect.KFunction

data class EventHandlerMethod(
    val listener: Any,
    val function: KFunction<*>,
    val priority: Int
)