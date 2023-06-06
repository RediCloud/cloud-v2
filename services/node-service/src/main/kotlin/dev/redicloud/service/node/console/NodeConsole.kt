package dev.redicloud.service.node.console

import dev.redicloud.console.Console
import dev.redicloud.service.node.NodeService
import kotlin.system.exitProcess

class NodeConsole(nodeService: NodeService) : Console("node", eventManager = nodeService.eventManager) {

    override fun onExit(exception: Exception?) {
        if (exception != null) {
            exception.printStackTrace()
            exitProcess(1) //TODO: NodeService#shutdown
        }
    }

}