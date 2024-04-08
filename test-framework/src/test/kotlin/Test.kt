import dev.redicloud.testing.RediCloud
import dev.redicloud.testing.pre.PreJavaVersion
import dev.redicloud.testing.utils.FileSelectStrategy
import dev.redicloud.testing.pre.PreServerVersion

fun main() {
    RediCloud.startCluster {
        nodes = 1
        name = "core-test"

        version {
            branch = "tests"
            build = "latest"
        }

        fileTemplate {
            prefix = "core"
            name = "spigot"
            gradleBuildFile {
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
            serverVersion = PreServerVersion.BUNGEECORD_LATEST
            minStartedServicesPerNode = 1
            exposePortRange(25565, 1)
        }
        configurationTemplate {
            name = "Lobby"
            maxMemory = 1024
            serverVersion = PreServerVersion.PAPER_1_20_4
            maxPlayers = 100
            minStartedServicesPerNode = 1
        }

        configureServerVersion {
            name = PreServerVersion.PAPER_1_20_4
            javaVersion = PreJavaVersion.JAVA_21
        }
        configureServerVersion {
            name = PreServerVersion.BUNGEECORD_LATEST
            javaVersion = PreJavaVersion.JAVA_21
        }
    }
    RediCloud.waitForExit()
}