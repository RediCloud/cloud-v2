package dev.redicloud.service.node.bootstrap

import dev.redicloud.logging.LogManager
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import dev.redicloud.utils.loadProperties
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = runBlocking {
    println("Starting node service...")
    runCatching {
        loadProperties(Thread.currentThread().contextClassLoader)
        val preConsole = InitializeConsole()
        val databaseConnection = preConsole.databaseConnection!!
        val databaseConfiguration = preConsole.databaseConfiguration!!
        val nodeConfiguration = preConsole.nodeConfiguration!!
        preConsole.close()
        NodeService(databaseConfiguration, databaseConnection, nodeConfiguration, preConsole.firstStartDetected)
    }.onFailure {
        LogManager.rootLogger().severe("Failed to start node service!", it)
        Thread.sleep(1000)
        exitProcess(1)
    }
}