package dev.redicloud.service.node.bootstrap

import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import dev.redicloud.utils.loadProperties
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    runBlocking {
        println("Starting node service...")
        try {
            loadProperties(Thread.currentThread().contextClassLoader)
            val preConsole = InitializeConsole()
            val databaseConnection = preConsole.databaseConnection!!
            val databaseConfiguration = preConsole.databaseConfiguration!!
            val serviceId = preConsole.serviceId!!
            val nodeConfiguration = preConsole.nodeConfiguration!!
            preConsole.close()
            NodeService(databaseConfiguration, databaseConnection, nodeConfiguration, preConsole.firstStartDetected)
        }catch (e: Exception) {
            println("Failed to start node service!")
            e.printStackTrace()
            Thread.sleep(1000)
            exitProcess(1)
        }
    }
}