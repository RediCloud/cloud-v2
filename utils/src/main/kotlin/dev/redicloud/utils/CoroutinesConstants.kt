package dev.redicloud.utils

import dev.redicloud.logging.LogManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val threadLogger = LogManager.logger("Coroutines")

val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    threadLogger.severe("Caught exception in coroutine-context: $coroutineContext", throwable)
}

val defaultScope = CoroutineScope(Dispatchers.Default + coroutineExceptionHandler)
val ioScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)