group = "dev.redicloud.connector"

dependencies {
    shade("org.redisson:redisson:${Versions.redisson}") {
        exclude("com.fasterxml.jackson.core")
    }
    shade("com.google.code.gson:gson:${Versions.gson}")
    shade("com.google.inject:guice:7.0.0")
    shade("io.netty:netty-handler:4.1.94.Final")
    shade("io.netty:netty-resolver-dns:4.1.94.Final")
    shade("io.netty:netty-resolver:4.1.94.Final")
    shade("io.netty:netty-transport:4.1.94.Final")
    shade("io.netty:netty-buffer:4.1.94.Final")
    shade("io.netty:netty-codec:4.1.94.Final")
    shade("io.netty:netty-common:4.1.94.Final")
}