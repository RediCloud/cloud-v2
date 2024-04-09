import dev.redicloud.testing.RediCloud
import dev.redicloud.testing.pre.PreJavaVersion
import dev.redicloud.testing.utils.FileSelectStrategy
import dev.redicloud.testing.pre.PreServerVersion

fun main() {
    RediCloud.startCluster {
        name = "core-test"

        // Set the cloud version that should be used (default: latest stable build)
        version {
            branch = "tests"
            build = "latest"
        }

        // Create a new node that will be automatically started and connected to the cloud
        node {
            name = "node-1"
            maxMemory = 3072
        }

        // Create a new file template where you can upload e.g. your plugin
        val spigotCoreTemplate = fileTemplate {
            prefix = "core"
            name = "spigot"
            // You can use 'gradleBuildFile' when you have a gradle project and want to upload the output jar file
            // Otherwise you can use 'file' to upload a file from your local machine
            gradleBuildFile {
                projectName = "examples/example-plugin"
                targetDirectory = "plugins"
                shadowJar = false
                selectStrategy = FileSelectStrategy.LATEST_MODIFIED
            }
        }

        // Create a new configuration template for a proxy server
        configurationTemplate {
            name = "Proxy"
            // You can configure all settings that are normally available in the cloud console for a configuration template
            // ...
            maxMemory = 512
            startPort = 25565
            serverVersion = PreServerVersion.BUNGEECORD_LATEST // Use PreServerVersion enum to get pre defined server versions
            minStartedServices = 1
            // Expose the ports for the template so that they can be accessed from the outside the container
            // You can also use 'exposePort' to expose a single port
            exposePortRange(25565, 1) // Expose the port 25565 (with range=3 it would expose 25565, 25566, 25567)
        }
        configurationTemplate {
            name = "Lobby"
            maxMemory = 1024
            serverVersion = PreServerVersion.PAPER_1_20_4
            maxPlayers = 100
            minStartedServicesPerNode = 1
            fallback = true // To connect to the fallback server
            fileTemplates = mutableListOf(spigotCoreTemplate) // Use the file template that was created before
            jvmArguments = mutableListOf(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" // Enable debugging e.g. for your plugin
            )
            exposePort(containerPort = 5005, hostPort = 5000) // Expose the debug port 5005 to your local machine on port 5000
        }

        // Set the java version for the server versions
        // Use PreJavaVersion enum to get all available java versions
        configureServerVersion {
            name = PreServerVersion.PAPER_1_20_4
            javaVersion = PreJavaVersion.JAVA_21
        }
        configureServerVersion {
            name = PreServerVersion.BUNGEECORD_LATEST
            javaVersion = PreJavaVersion.JAVA_21
        }

        // You can define shortcuts that can be executed from the console
        shortcut("upload example-plugin") {
            // Upload the example plugin to the spigot core template and restart the lobby
            RediCloud.uploadGradleBuildFileToNodes {
                projectName = "examples/example-plugin"
                targetDirectory = "${spigotCoreTemplate.path}/plugins"
                selectStrategy = FileSelectStrategy.LATEST_MODIFIED
            }
            it.execute("server stop Lobby-*") // You can also execute commands on the cloud
        }

    }
    // Will block the main thread until the user stops the process with 'exit'
    RediCloud.userInputs()
}