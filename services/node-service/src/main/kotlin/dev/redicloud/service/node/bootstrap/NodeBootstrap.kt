package dev.redicloud.service.node.bootstrap

import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import dev.redicloud.utils.loadProperties
import dev.redicloud.utils.threadLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val STARTUP_DELAY = 500.milliseconds

@Suppress("TooGenericExceptionCaught")
fun main(args: Array<String>) {
    println("Starting node service...")
    Thread.sleep(STARTUP_DELAY.inWholeMilliseconds)
    Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
        threadLogger.severe("Caught exception in thread: ${thread.name}", throwable)
    }
    runBlocking {
        try {
            loadProperties(Thread.currentThread().contextClassLoader)
            val preConsole = InitializeConsole()
            val databaseConnection = preConsole.databaseConnection!!
            val databaseConfiguration = preConsole.databaseConfiguration!!
            val nodeConfiguration = preConsole.nodeConfiguration!!
            val nodeService = NodeService(
                databaseConfiguration,
                databaseConnection,
                nodeConfiguration,
                preConsole.firstStartDetected
            )
            nodeService.start()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogManager.rootLogger().severe("Failed to start node service!", e)
            delay(1.seconds)
            exitProcess(1)
        }
    }
}
