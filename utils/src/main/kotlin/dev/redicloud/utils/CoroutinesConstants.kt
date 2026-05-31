package dev.redicloud.utils

import dev.redicloud.logging.LogManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val threadLogger = LogManager.logger("Coroutines")

val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    threadLogger.severe("Caught exception in coroutine-context: $coroutineContext", throwable)
}

@Deprecated("Use a service-scoped CoroutineScope instead", ReplaceWith("serviceScope"))
val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)

@Deprecated("Use a service-scoped CoroutineScope instead", ReplaceWith("serviceScope"))
val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
