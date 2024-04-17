package dev.redicloud.module.webinterface

import dev.redicloud.testing.RediCloud
import java.util.logging.Logger

fun main() {
    RediCloud.startCluster {
        name = "webinterface-test"

        node {
            name = "node01"
            maxMemory = 3068
        }

        shortcut("reload") {
            RediCloud.uploadGradleBuildFileToNodes {
                projectName = "modules/webinterface"
                targetDirectory = "storage/modules"
                shadowJar = true
            }
            it.execute("module unload webinterface")
            it.execute("module load webinterface")
            Logger.getGlobal().info("Uploaded webinterface file to node and executed module reload command.")
        }
    }
}