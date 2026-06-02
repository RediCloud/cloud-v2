package dev.redicloud.utils

import dev.redicloud.logging.LogManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val threadLogger = LogManager.logger("Coroutines")

val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    threadLogger.severe("Caught exception in coroutine-context: $coroutineContext", throwable)
}

@Deprecated("Use a service-scoped CoroutineScope instead", ReplaceWith("serviceScope"))
val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)

@Deprecated("Use a service-scoped CoroutineScope instead", ReplaceWith("serviceScope"))
val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

/**
 * Conditionally acquires the mutex before executing [action].
 * Used as a re-entrancy workaround: callers pass `lock = false` when the mutex is already held.
 */
suspend inline fun <T> Mutex.withOptionalLock(lock: Boolean, action: () -> T): T {
    return if (lock) withLock { action() } else action()
}
