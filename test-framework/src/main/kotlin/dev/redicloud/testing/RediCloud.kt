package dev.redicloud.testing

import dev.redicloud.testing.config.ClusterConfiguration

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

    fun waitForExit() {
        while (true) {
            val input = readlnOrNull() ?: return
            if (input == "exit") {
                clusters.toSet().forEach {
                    stopCluster(it)
                }
                return
            }
        }
    }

}