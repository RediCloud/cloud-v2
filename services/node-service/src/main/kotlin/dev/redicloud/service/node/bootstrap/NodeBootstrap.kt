package dev.redicloud.service.node.bootstrap

import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import dev.redicloud.utils.loadProperties
import dev.redicloud.utils.threadLogger
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

private const val STARTUP_DELAY_MS = 500L
private const val FAILURE_RETRY_DELAY_MS = 1000L

fun main(args: Array<String>) {
    println("Starting node service...")
    Thread.sleep(STARTUP_DELAY_MS)
    Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
        threadLogger.severe("Caught exception in thread: ${thread.name}", throwable)
    }
    runBlocking {
        runCatching {
            loadProperties(Thread.currentThread().contextClassLoader)
            val preConsole = InitializeConsole()
            val databaseConnection = preConsole.databaseConnection!!
            val databaseConfiguration = preConsole.databaseConfiguration!!
            val nodeConfiguration = preConsole.nodeConfiguration!!
            NodeService(databaseConfiguration, databaseConnection, nodeConfiguration, preConsole.firstStartDetected)
        }.onFailure {
            LogManager.rootLogger().severe("Failed to start node service!", it)
            Thread.sleep(FAILURE_RETRY_DELAY_MS)
            exitProcess(1)
        }
    }
}