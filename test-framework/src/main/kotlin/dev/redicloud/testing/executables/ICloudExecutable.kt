package dev.redicloud.testing.executables

import dev.redicloud.testing.RediCloudCluster

interface ICloudExecutable {
    fun preApply(cluster: RediCloudCluster) {}
    fun apply(cluster: RediCloudCluster)
}