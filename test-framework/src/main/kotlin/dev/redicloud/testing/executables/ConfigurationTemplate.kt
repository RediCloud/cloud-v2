package dev.redicloud.testing.executables

import dev.redicloud.api.utils.TEMPLATE_FOLDER
import dev.redicloud.api.utils.toUniversalPath
import dev.redicloud.testing.RediCloudCluster
import dev.redicloud.testing.RediCloudNode
import dev.redicloud.testing.pre.PreServerVersion
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class ConfigurationTemplate(
    var name: String,
    var serverVersion: PreServerVersion,
    var maxMemory: Long = 1024,
    var fileTemplates: MutableList<FileTemplate> = mutableListOf(),
    var nodes: MutableList<String> = mutableListOf(),
    var minStartedServices: Int = 1,
    var maxStartedServices: Int = -1,
    var minStartedServicesPerNode: Int = -1,
    var maxStartedServicesPerNode: Int = -1,
    var percentToStartNewService: Double = 100.0,
    var serverSplitter: String = "-",
    var fallback: Boolean = false,
    var startPriority: Int = if (fallback) 0 else 50,
    var static: Boolean = false,
    var startPort: Int = 4000,
    var joinPermission: String? = null,
    var maxPlayers: Int = 100,
    var timeAfterStopUselessServer: Duration = 5.minutes,
    var jvmArguments: MutableList<String> = mutableListOf(),
    var environmentVariables: MutableMap<String, String> = mutableMapOf(),
    var programParameters: MutableList<String> = mutableListOf(),
    var defaultFiles: MutableMap<String, String> = mutableMapOf(),
    var fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    var exposedPorts: MutableMap<String, Int> = mutableMapOf()
) : ICloudExecutable {

    override fun preApply(cluster: RediCloudCluster) {
        cluster.nodes.forEach { node ->
            exposedPorts.map { it.key.toInt() to it.value }.forEach { (hostPort, containerPort) ->
                node.exposeFixedPort(hostPort, containerPort)
            }
        }
    }

    override fun apply(cluster: RediCloudCluster) {
        val node = cluster.nodes.first()
        fun execute(command: String) {
            node.execute("ct $command")
            Thread.sleep(RediCloudNode.CONSOLE_COMMAND_DELAY.inWholeMilliseconds)
        }

        fun executeEdit(command: String) {
            node.execute("ct edit $name $command")
            Thread.sleep(RediCloudNode.CONSOLE_COMMAND_DELAY.inWholeMilliseconds)
        }
        execute("create $name")
        executeEdit("version ${serverVersion.versionName}")
        executeEdit("maxMemory $maxMemory")
        executeEdit("splitter $serverSplitter")
        executeEdit("fallback $fallback")
        executeEdit("startPriority $startPriority")
        executeEdit("static $static")
        executeEdit("startPort $startPort")
        executeEdit("permission $joinPermission")
        executeEdit("maxPlayers $maxPlayers")
        executeEdit("stoptime ${timeAfterStopUselessServer.inWholeMinutes}")
        jvmArguments.forEach { executeEdit("ja add $it") }
        environmentVariables.forEach { (key, value) -> executeEdit("env add $key $value") }
        programParameters.forEach { executeEdit("programparameter add $it") }
        fileEdits.forEach { (file, edits) ->
            edits.forEach { (key, value) ->
                executeEdit("fileedits add $file $key $value")
            }
        }
        defaultFiles.forEach { (file, path) -> executeEdit("files add $file $path") }
        fileTemplates.forEach { executeEdit("filetemplates add ${it.displayName}") }
        nodes.forEach { executeEdit("nodes add $it") }

        executeEdit("maxServices $maxStartedServices")
        executeEdit("maxServicesPerNode $maxStartedServicesPerNode")
        executeEdit("percentToStartNew $percentToStartNewService")
        executeEdit("minServices $minStartedServices")
        executeEdit("minServicesPerNode $minStartedServicesPerNode")
    }

    fun exposePortRange(hostStartPort: Int, range: Int) {
        for (i in 0 until range) {
            exposedPorts["${hostStartPort+i}"] = startPort + i
        }
    }

    fun exposePort(containerPort: Int, hostPort: Int = containerPort) {
        exposedPorts["$hostPort"] = containerPort
    }

}