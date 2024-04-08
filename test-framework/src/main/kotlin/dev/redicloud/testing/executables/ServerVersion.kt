package dev.redicloud.testing.executables

import dev.redicloud.testing.RediCloudCluster
import dev.redicloud.testing.pre.PreJavaVersion
import dev.redicloud.testing.pre.PreServerVersion

class ServerVersion(
    var name: PreServerVersion,
    var javaVersion: PreJavaVersion,
) : ICloudExecutable{
    override fun apply(cluster: RediCloudCluster) {
        val node = cluster.nodes.first()
        node.execute("sv edit ${name.versionName} javaversion ${javaVersion.versionName}")
        Thread.sleep(100)
    }
}