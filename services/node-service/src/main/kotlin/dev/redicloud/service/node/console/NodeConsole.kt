package dev.redicloud.service.node.console

import dev.redicloud.console.Console
import dev.redicloud.event.EventManager
import dev.redicloud.service.node.NodeConfiguration

class NodeConsole(nodeConfiguration: NodeConfiguration, eventManager: EventManager) :
    Console(nodeConfiguration.nodeName, eventManager, true) {



}