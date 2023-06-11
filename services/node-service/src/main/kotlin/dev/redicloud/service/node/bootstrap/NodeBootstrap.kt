package dev.redicloud.service.node.bootstrap

import dev.redicloud.repository.server.version.MinecraftVersion
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole
import dev.redicloud.utils.versions.JavaVersion
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        println("Starting node service...")
        MinecraftVersion.loadIfNotLoaded()
        JavaVersion.loadIfNotLoaded()
        val preConsole = InitializeConsole()
        val databaseConnection = preConsole.databaseConnection!!
        val databaseConfiguration = preConsole.databaseConfiguration!!
        val serviceId = preConsole.serviceId!!
        val nodeConfiguration = preConsole.nodeConfiguration!!
        preConsole.close()
        NodeService(databaseConfiguration, databaseConnection, nodeConfiguration)
    }
}