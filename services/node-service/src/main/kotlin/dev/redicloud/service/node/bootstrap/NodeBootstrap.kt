package dev.redicloud.service.node.bootstrap

import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        println("Starting node service...")
        val preConsole = InitializeConsole()
        val databaseConnection = preConsole.databaseConnection!!
        val databaseConfiguration = preConsole.databaseConfiguration!!
        val serviceId = preConsole.serviceId!!
        val nodeConfiguration = preConsole.nodeConfiguration!!
        preConsole.close()
        NodeService(databaseConfiguration, databaseConnection, nodeConfiguration, preConsole.firstStartDetected)
    }
}