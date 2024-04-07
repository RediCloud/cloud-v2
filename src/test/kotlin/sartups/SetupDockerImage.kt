package sartups

import cloud.NODE_IMAGE_BUILD
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

fun main() {
    val container = GenericContainer(NODE_IMAGE_BUILD)
    container.start()
    container.waitingFor(Wait.forHealthcheck())
    container.stop()
}