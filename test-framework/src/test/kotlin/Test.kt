import dev.redicloud.testing.RediCloud
import dev.redicloud.testing.utils.FileSelectStrategy
import dev.redicloud.testing.utils.PreServerVersion
import java.io.File

fun main() {
    RediCloud.startCluster {
        nodes = 1
        name = "core-test"

        version {
            branch = "dev"
            build = "latest"
        }

        /*
        fileTemplate {
            prefix = "core"
            name = "spigot"
            file {
                file = File("static/WorldEdit.jar")
            }
        }
        fileTemplate {
            prefix = "core"
            name = "proxy"
            projectFile {
                projectName = "api-impl/proxy"
                shadowJar = true
                selectStrategy = FileSelectStrategy.LATEST_MODIFIED
            }
        }

        configurationTemplate {
            name = "Proxy"
            maxMemory = 512
            startPort = 25565
            serverVersion = PreServerVersion.VELOCITY_LATEST.name
        }
        configurationTemplate {
            name = "Lobby"
            maxMemory = 1024
            serverVersion = PreServerVersion.PAPER_1_8_8.name
            maxPlayers = 100
            minStartedServicesPerNode = 1
        }
         */
    }
    RediCloud.waitForExit()
}