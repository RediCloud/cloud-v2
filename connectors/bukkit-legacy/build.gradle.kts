plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.connector"

dependencies {
    shade(libs.redisson) {
        exclude("com.fasterxml.jackson.core")
    }
    shade(libs.gson)
    shade(libs.guice)
    shade(libs.netty.handler)
    shade(libs.netty.resolver.dns)
    shade(libs.netty.resolver)
    shade(libs.netty.transport)
    shade(libs.netty.buffer)
    shade(libs.netty.codec)
    shade(libs.netty.common)
    shade(libs.logback.core.legacy)
    shade(libs.logback.classic.legacy)
    dependency(libs.adventure.api)
    dependency(libs.adventure.bukkit)
}