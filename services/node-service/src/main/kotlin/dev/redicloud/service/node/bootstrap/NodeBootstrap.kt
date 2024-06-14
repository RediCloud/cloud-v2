package dev.redicloud.service.node.bootstrap

import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import dev.redicloud.updater.Updater
import dev.redicloud.utils.coroutineExceptionHandler
import dev.redicloud.utils.loadProperties
import dev.redicloud.utils.threadLogger
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println("Starting node service...")
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
            Updater.postUpdate(preConsole, databaseConnection)
            NodeService(databaseConfiguration, databaseConnection, nodeConfiguration, preConsole.firstStartDetected)
        }.onFailure {
            LogManager.rootLogger().severe("Failed to start node service!", it)
            Thread.sleep(1000)
            exitProcess(1)
        }
    }
}