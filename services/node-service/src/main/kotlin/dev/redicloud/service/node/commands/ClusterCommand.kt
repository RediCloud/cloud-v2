package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.service.base.repository.pingService
import dev.redicloud.service.base.suggester.ConnectedCloudNodeSuggester
import dev.redicloud.service.node.NodeService
import dev.redicloud.service.node.repository.node.suspendNode
import dev.redicloud.utils.*
import dev.redicloud.api.service.ServiceId
import dev.redicloud.console.utils.toConsoleValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Command("cluster")
@CommandDescription("All commands for the cluster")
class ClusterCommand(private val nodeService: NodeService) : ICommand {

    @CommandSubPath("nodes")
    @CommandAlias(["list", "info"])
    @CommandDescription("List all nodes")
    fun list(actor: ConsoleActor) {
        runBlocking {
            try {
                val nodes = nodeService.nodeRepository.getRegisteredNodes()
                actor.sendHeader("Nodes")
                nodes.forEach { node ->
                    actor.sendMessage("")
                    actor.sendMessage("§8> §a${if (node.master) node.identifyName() + " §8(§6master§8)" else node.identifyName()}")
                    actor.sendMessage("   - Status§8: %hc%${
                        if (node.suspended) "§4● §8(§fsuspended§8)"
                        else if (node.connected) "§2● §8(§fconnected§8)"
                        else "§c● §8(§fdisconnected§8)"
                    }")
                    actor.sendMessage("   - Memory§8: %hc%${node.currentMemoryUsage} §8/ %hc%${node.maxMemory}")
                    actor.sendMessage("   - IP§8: %hc%${node.currentOrLastSession()?.ipAddress ?: "Unknown"}")
                    if (node.connected) {
                        sendPingMessage(node, actor, "   - Ping§8: ")
                        val server = node.hostedServers.mapNotNull { nodeService.serverRepository.getServer<CloudServer>(it)?.name }
                        actor.sendMessage("   - Server§8: %hc%${if (server.isEmpty()) "None" else server.joinToString(", ")}")
                    }
                }
                actor.sendMessage("")
                actor.sendHeader("Nodes")
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @CommandSubPath("ping <node>")
    @CommandDescription("Ping a node")
    fun ping(
        actor: ConsoleActor,
        @CommandParameter("node", true, ConnectedCloudNodeSuggester::class) node: CloudNode
    ) {
        if (!node.connected) {
            actor.sendMessage("${node.identifyName()} is not connected to the cluster!")
            return
        }
        sendPingMessage(node, actor, "${node.identifyName()} §8> §7")
    }

    @CommandSubPath("templates push <node>")
    @CommandDescription("Push templates to the cluster")
    fun pushTemplates(
        actor: ConsoleActor,
        @CommandParameter("node", true, ConnectedCloudNodeSuggester::class) node: CloudNode
    ) {
        if (!node.connected) {
            actor.sendMessage("${node.identifyName()} is not connected to the cluster!")
            return
        }
        runBlocking { nodeService.fileTemplateRepository.pushTemplates(node.serviceId) }
    }

    private val suspendConfirm = mutableMapOf<ServiceId, Long>()
    @CommandSubPath("suspend <node>")
    @CommandDescription("Suspend a node")
    fun suspend(
        actor: ConsoleActor,
        @CommandParameter("node", true, ConnectedCloudNodeSuggester::class) node: CloudNode
    ) {
        runBlocking {
            if (suspendConfirm.contains(node.serviceId) && System.currentTimeMillis() - suspendConfirm[node.serviceId]!! < 15000) {
                actor.sendMessage("Suspending node ${node.identifyName()}...")
                nodeService.nodeRepository.suspendNode(nodeService, node.serviceId)
                return@runBlocking
            }
            actor.sendHeader("Suspend")
            actor.sendMessage("")
            actor.sendMessage("Node§8: %hc%${node.identifyName()}")
            actor.sendMessage("Servers§8: %hc%${node.hostedServers.mapNotNull { nodeService.serverRepository.getServer<CloudServer>(it)?.name }.joinToString(", ")}")
            sendPingMessage(node, actor, "Ping§8: %hc%")
            actor.sendMessage("")
            actor.sendMessage("§cThis will suspend the node and all hosted servers will be stopped!")
            actor.sendMessage("§cEnter the command again to confirm within 15 seconds")
            actor.sendMessage("")
            actor.sendHeader("Suspend")
            suspendConfirm[node.serviceId] = System.currentTimeMillis()
        }
    }

    @CommandSubPath("edit <node> maxmemory <value>")
    @CommandDescription("Edit the max memory of a node")
    fun editMaxMemory(
        actor: ConsoleActor,
        @CommandParameter("node", true, ConnectedCloudNodeSuggester::class) node: CloudNode,
        @CommandParameter("memory", true, MemorySuggester::class) memory: Long
    ) = defaultScope.launch {
        if (node.currentMemoryUsage > memory) {
            actor.sendMessage("§cThe memory usage of ${node.identifyName()} is higher than the new max memory!")
            actor.sendMessage("§cPlease stop some servers hosted on the node before changing the max memory!")
            return@launch
        }
        node.maxMemory = memory
        nodeService.nodeRepository.updateNode(node)
        actor.sendMessage("The max memory of ${node.identifyName()} has been updated to ${toConsoleValue(memory)}!")
    }

    private val deleteConfirm = mutableMapOf<ServiceId, Long>()
    @CommandSubPath("delete <node>")
    @CommandDescription("Delete a node")
    fun delete(
        actor: ConsoleActor,
        @CommandParameter("node", true, ConnectedCloudNodeSuggester::class) node: CloudNode
    ) = defaultScope.launch {
        if (node.connected) {
            actor.sendMessage("§cThe node ${node.identifyName()} is still connected to the cluster!")
            actor.sendMessage("§cPlease disconnect the node before deleting it!")
            return@launch
        }
        if (deleteConfirm.contains(node.serviceId) && System.currentTimeMillis() - deleteConfirm[node.serviceId]!! < 15000) {
            actor.sendMessage("Deleting node ${node.identifyName()}...")
            nodeService.nodeRepository.deleteNode(node.serviceId)
            return@launch
        }
        actor.sendMessage("§cThis will delete the node! The cloud files on the remote server will not be deleted!")
        actor.sendMessage("§cEnter the command again to confirm within 15 seconds")
        deleteConfirm[node.serviceId] = System.currentTimeMillis()
    }

    private fun sendPingMessage(node: CloudNode, actor: ConsoleActor, prefix: String, block: (Long) -> Unit = {}) {
        var ping = -2L
        val local = node.serviceId == nodeService.serviceId
        var cancel = false
        val pingAnimation = AnimatedLineAnimation(actor.console, 200) {
            if (cancel) {
                null
            }else if(local){
                cancel = true
                "$prefix%hc%this node"
            }else if (ping == -2L) {
                "$prefix%hc%%loading% §8(%tc%pinging§8)"
            } else if (ping == -1L) {
                cancel = true
                "$prefix%hc%§cping time outed"
            } else {
                cancel = true
                "$prefix%hc%$ping%tc%ms"
            }
        }
        actor.console.startAnimation(pingAnimation)
        if (local) return
        defaultScope.launch {
            delay(1500)
            ping = nodeService.nodeRepository.pingService(node.serviceId)
            block(ping)
        }
    }

}