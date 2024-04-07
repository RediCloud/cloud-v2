package sartups

import cloud.RediCloudCluster
import java.util.*

fun main() {
    val cloudName = UUID.randomUUID().toString().substring(0, 4)
    val cloud = RediCloudCluster(
        cloudName,
        1,
        true,
        "2.2.1-SNAPSHOT"
    )
    while (true) {
        val input = readlnOrNull() ?: continue
        if (input == "exit") {
            cloud.stop()
            break
        }
        cloud.nodes.forEach {
            it.process.execute(input)
        }
    }
}