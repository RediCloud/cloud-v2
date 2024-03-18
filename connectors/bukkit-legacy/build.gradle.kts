group = "dev.redicloud.connector"

dependencies {
    shade("org.redisson:redisson:${Versions.redisson}") {
        exclude("com.fasterxml.jackson.core")
    }
    shade("com.google.code.gson:gson:${Versions.gson}")
    shade("com.google.inject:guice:${Versions.guice}")
    shade("io.netty:netty-handler:${Versions.netty}")
    shade("io.netty:netty-resolver-dns:${Versions.netty}")
    shade("io.netty:netty-resolver:${Versions.netty}")
    shade("io.netty:netty-transport:${Versions.netty}")
    shade("io.netty:netty-buffer:${Versions.netty}")
    shade("io.netty:netty-codec:${Versions.netty}")
    shade("io.netty:netty-common:${Versions.netty}")

    shade("ch.qos.logback:logback-core:1.3.14")
    shade("ch.qos.logback:logback-classic:1.5.3")
}