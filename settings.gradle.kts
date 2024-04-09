rootProject.name = "RediCloud-v2"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.redicloud.dev/releases/")
    }
}

include("database")
include("packets")
include("utils")
include("events")
include("console")
include("logging")
include("tasks")
include("file-cluster")
include("cache")


include("server-factories:node-server-factory")
findProject(":server-factories:node-server-factory")?.name = "node-server-factory"

include("apis:base-api")
findProject(":apis:base-api")?.name = "base-api"

include("services:base-service")
findProject(":services:base-service")?.name = "base-service"

include("services:node-service")
findProject(":services:node-service")?.name = "node-service"

include("repositories:service-repository")
findProject(":repositories:service-repository")?.name = "service-repository"

include("repositories:node-repository")
findProject(":repositories:node-repository")?.name = "node-repository"

include("repositories:server-repository")
findProject(":repositories:server-repository")?.name = "server-repository"

include("repositories:java-version-repository")
findProject(":repositories:java-version-repository")?.name = "java-version-repository"

include("repositories:configuration-template-repository")
findProject(":repositories:configuration-template-repository")?.name = "configuration-template-repository"

include("repositories:server-version-repository")
findProject(":repositories:server-version-repository")?.name = "server-version-repository"

include("repositories:file-template-repository")
findProject(":repositories:file-template-repository")?.name = "file-template-repository"

include("commands:command-api")
findProject(":commands:command-api")?.name = "command-api"

include("connectors:bukkit-connector")
findProject(":connectors:bukkit-connector")?.name = "bukkit-connector"

include("connectors:velocity-connector")
findProject(":connectors:velocity-connector")?.name = "velocity-connector"

include("connectors:bungeecord-connector")
findProject(":connectors:bungeecord-connector")?.name = "bungeecord-connector"

include("services:minecraft-server-service")
findProject(":services:minecraft-server-service")?.name = "minecraft-server-service"

include("services:proxy-server-service")
findProject(":services:proxy-server-service")?.name = "proxy-server-service"

include("repositories:player-repository")
findProject(":repositories:player-repository")?.name = "player-repository"

include("connectors:bukkit-legacy")
findProject(":connectors:bukkit-legacy")?.name = "bukkit-legacy"

include("repositories:cache-repository")
findProject(":repositories:cache-repository")?.name = "cache-repository"

include("examples:example-plugin")
findProject(":examples:example-plugin")?.name = "example-plugin"

include("modules:module-handler")
findProject(":modules:module-handler")?.name = "module-handler"

include("examples:example-module")
findProject(":examples:example-module")?.name = "example-module"

include("modules:papermc-updater")
findProject(":modules:papermc-updater")?.name = "papermc-updater"

include("apis:node-api")
findProject(":apis:node-api")?.name = "node-api"

include("server-factories:remote-server-factory")
findProject(":server-factories:remote-server-factory")?.name = "remote-server-factory"

include("connectors:minestom-connector")
findProject(":connectors:minestom-connector")?.name = "minestom-connector"

include("updater")
findProject(":updater")?.name = "updater"

include("apis:connector-api")
findProject(":apis:connector-api")?.name = "connector-api"

include("modules:reset-module")
findProject(":modules:reset-module")?.name = "reset-module"

include("test-framework")
findProject(":test-framework")?.name = "test-framework"