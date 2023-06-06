package dev.redicloud.service.node.bootstrap

import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.console.InitializeConsole

fun main(args: Array<String>) {
    val preConsole = InitializeConsole()
    val databaseConfiguration = preConsole.databaseConfiguration!!
    val serviceId = preConsole.serviceId!!
    val nodeConfiguration = preConsole.nodeConfiguration!!
    NodeService(databaseConfiguration, nodeConfiguration)
}