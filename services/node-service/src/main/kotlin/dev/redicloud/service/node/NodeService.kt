package dev.redicloud.service.node

import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.cluster.file.FileNodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.events.NodeDisconnectEvent
import dev.redicloud.service.base.events.NodeSuspendedEvent
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.repository.node.connect
import dev.redicloud.service.node.repository.node.disconnect
import dev.redicloud.service.node.commands.*
import dev.redicloud.service.node.tasks.node.NodeChooseMasterTask
import dev.redicloud.service.node.tasks.NodePingTask
import dev.redicloud.service.node.tasks.NodeSelfSuspendTask
import dev.redicloud.utils.TEMP_FOLDER
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class NodeService(
    databaseConfiguration: DatabaseConfiguration,
    databaseConnection: DatabaseConnection,
    val configuration: NodeConfiguration,
    val firstStart: Boolean = false
) : BaseService(databaseConfiguration, databaseConnection, configuration.toServiceId()) {

    val console: NodeConsole = NodeConsole(configuration, eventManager, nodeRepository)
    val fileNodeRepository: FileNodeRepository
    val fileCluster: FileCluster

    init {
        fileNodeRepository = FileNodeRepository(databaseConnection, packetManager)
        fileCluster = FileCluster(configuration.hostAddress, fileNodeRepository, packetManager, nodeRepository, eventManager)
        runBlocking {
            this@NodeService.initShutdownHook()

            nodeRepository.connect(this@NodeService)
            try { memoryCheck() } catch (e: Exception) {
                LOGGER.severe("Error while checking memory", e)
                shutdown()
                return@runBlocking
            }

            try { this@NodeService.checkJavaVersions() } catch (e: Exception) {
                LOGGER.warning("Error while checking java versions", e)
            }
            this@NodeService.registerPreTasks()
            this@NodeService.connectFileCluster()
            this@NodeService.registerPackets()
            this@NodeService.registerCommands()
            this@NodeService.registerTasks()
        }
    }

    override fun shutdown() {
        if (SHUTTINGDOWN) return
        SHUTTINGDOWN = true
        LOGGER.info("Shutting down node service...")
        runBlocking {
            fileCluster.disconnect(true)
            nodeRepository.disconnect(this@NodeService)
            super.shutdown()
            TEMP_FOLDER.getFile().deleteRecursively()
        }
    }

    private fun registerTasks() {
        taskManager.builder()
            .task(NodePingTask(this))
            .instant()
            .event(NodeDisconnectEvent::class)
            .period(10.seconds)
            .register()
        taskManager.builder()
            .task(NodeSelfSuspendTask(this))
            .event(NodeSuspendedEvent::class)
            .period(10.seconds)
            .register()
    }

    private fun registerPreTasks() {
        taskManager.builder()
            .task(NodeChooseMasterTask(nodeRepository))
            .instant()
            .event(NodeDisconnectEvent::class)
            .event(NodeSuspendedEvent::class)
            .register()
    }

    private suspend fun checkJavaVersions() {
        val detected = mutableListOf<JavaVersion>()
        javaVersionRepository.detectInstalledVersions().forEach {
            if (javaVersionRepository.existsVersion(it.name)) return@forEach
            javaVersionRepository.createVersion(it)
            detected.add(it)
        }
        if (detected.isNotEmpty()) {
            LOGGER.info("Detected %hc%${detected.size} %tc%java versions§8: %hc%${detected.joinToString("§8, %hc%") { it.name }}")
        }
        var wrongAutoDetectPossible = false
        javaVersionRepository.getVersions().forEach { version ->
            val located = version.autoLocate()
            if (located != null) {
                if (located.absolutePath == version.located[configuration.toServiceId().id]) return@forEach
                LOGGER.info("Auto located java version §8'%tc%${version.name}§8'%tc% at §8'%hc%${located.absolutePath}§8'")
                version.located[configuration.toServiceId().id] = located.absolutePath
                javaVersionRepository.updateVersion(version)
                return@forEach
            }
            if (!version.onlineVersion) {
                LOGGER.warning("Java version §8'%tc%${version.name}§8'%tc% is not installed!")
                wrongAutoDetectPossible = true
            }
        }
        if (wrongAutoDetectPossible) {
            LOGGER.warning("§cIf the version is installed, try to set the java home manually with '-Dredicloud.java.versions.path=path/to/java/versions'")
        }
    }

    private suspend fun memoryCheck() {
        val thisNode = nodeRepository.getNode(this.serviceId)!!
        if (thisNode.maxMemory < 1024) throw IllegalStateException("Max memory of this node is too low! Please increase the max memory of this node!")
        if (thisNode.maxMemory > Runtime.getRuntime().freeMemory()) throw IllegalStateException("Not enough memory available! Please increase the max memory of this node!")
    }

    private fun registerPackets() {
    }

    private suspend fun connectFileCluster() {
        try {
            this.fileCluster.connect()
            LOGGER.info("Connected to file cluster on port ${this.fileCluster.port}!")
        }catch (e: Exception) {
            LOGGER.severe("Failed to connect to file cluster!", e)
            this.shutdown()
            return
        }
    }

    private fun registerCommands() {
        console.commandManager.register(ExitCommand(this))
        console.commandManager.register(ClusterCommand(this))
        console.commandManager.register(CloudServerVersionCommand(this.serverVersionRepository, this.serverVersionTypeRepository, this.configurationTemplateRepository, this.serverRepository, this.console))
        console.commandManager.register(CloudServerVersionTypeCommand(this.serverVersionTypeRepository, this.configurationTemplateRepository, this.serverVersionRepository))
        console.commandManager.register(JavaVersionCommand(this.javaVersionRepository, this.configurationTemplateRepository))
    }

    private fun initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { this.shutdown() })
    }

}