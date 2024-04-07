import dev.redicloud.testing.RediCloud
import dev.redicloud.testing.utils.FileSelectStrategy
import dev.redicloud.testing.utils.PreServerVersion
import java.io.File

fun main() {
    RediCloud.startCluster {
        nodes = 1
        name = "core-test"

        version {
            branch = "tests"
            build = "31"
        }

        fileTemplate {
            prefix = "core"
            name = "spigot"
            projectFile {
                projectName = "examples/example-plugin"
                targetDirectory = "plugins"
                shadowJar = false
                selectStrategy = FileSelectStrategy.LATEST_MODIFIED
            }
        }

        configurationTemplate {
            name = "Proxy"
            maxMemory = 512
            startPort = 25565
            serverVersion = PreServerVersion.BUNGEECORD_LATEST.name
            minStartedServicesPerNode = 1
        }
        configurationTemplate {
            name = "Lobby"
            maxMemory = 1024
            serverVersion = PreServerVersion.PAPER_1_20_4.name
            maxPlayers = 100
            minStartedServicesPerNode = 1
        }
    }
    RediCloud.waitForExit()
}