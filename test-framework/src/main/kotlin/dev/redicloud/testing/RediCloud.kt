package dev.redicloud.testing

import dev.redicloud.testing.config.ClusterConfiguration
import dev.redicloud.testing.utils.FileSelectStrategy
import dev.redicloud.testing.utils.ProjectFileSelect
import org.testcontainers.images.builder.Transferable

object RediCloud {

    private val clusters = mutableListOf<RediCloudCluster>()

    fun startCluster(
        block: ClusterConfiguration.() -> Unit
    ): RediCloudCluster {
        return RediCloudCluster(ClusterConfiguration().apply(block)).also {
            clusters.add(it)
        }
    }

    fun stopCluster(cluster: RediCloudCluster) {
        cluster.unregisterNodes()
        clusters.remove(cluster)
    }

    fun userInputs() {
        while (true) {
            val input = readlnOrNull() ?: return
            if (input == "exit") {
                clusters.toSet().forEach {
                    stopCluster(it)
                }
                return
            }
            clusters.forEach { cluster ->
                cluster.config.shortcuts.forEach { shortcut ->
                    if (input == shortcut.key) {
                        shortcut.value(cluster)
                    }
                }
            }
        }
    }

    fun uploadGradleBuildFileToNodes(block: ProjectFileSelect.() -> Unit) {
        val select = ProjectFileSelect("", null, "", FileSelectStrategy.LATEST_MODIFIED, false).apply(block)
        val file = select.file
        val targetDirectory = select.targetDirectory
        if (!file.exists()) {
            throw IllegalArgumentException("File ${file.absolutePath} does not exist")
        }
        clusters.forEach { cluster ->
            cluster.nodes.forEach { node ->
                val containerPath = "${node.workingDirectory}/$targetDirectory/${file.name}"
                RediCloudNode.LOGGER.info("Uploading file ${file.absolutePath} to $containerPath (${node.config.name}@${cluster.config.name})...")
                node.copyFileToContainer(
                    Transferable.of(file.absolutePath),
                    containerPath
                )
            }
        }
    }

}