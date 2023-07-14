group = "dev.redicloud.connector.bukkit.legacy"

dependencies {
    shade("org.redisson:redisson:${Versions.redisson}") {
        exclude("com.fasterxml.jackson.core")
    }
    shade("com.google.code.gson:gson:${Versions.gson}")
    shade("io.netty:netty-handler:4.1.94.Final")
    shade("io.netty:netty-resolver-dns:4.1.94.Final")
    shade("io.netty:netty-resolver:4.1.94.Final")
    shade("io.netty:netty-transport:4.1.94.Final")
    shade("io.netty:netty-buffer:4.1.94.Final")
    shade("io.netty:netty-codec:4.1.94.Final")
    shade("io.netty:netty-common:4.1.94.Final")
}