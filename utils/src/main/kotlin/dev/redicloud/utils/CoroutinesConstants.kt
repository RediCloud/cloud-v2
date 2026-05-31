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

val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)
val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
