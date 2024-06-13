object BuildDependencies {
    const val CLOUD_VERSION = "2.3.4-RELEASE"
    const val CLOUD_LIBLOADER_VERSION = "1.7.0"
    const val CLOUD_LIBLOADER_BOOTSTRAP = "dev.redicloud.libloader:libloader-bootstrap:1.7.0"

    const val SPIGOT_API = "org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT"
    const val BUNGEECORD_API = "net.md-5:bungeecord-api:1.20-R0.3-SNAPSHOT"
    const val VELOCITY_API = "com.velocitypowered:velocity-api:3.1.1"
    const val MINESTOM_API = "dev.hollowcube:minestom-ce:1619cedc53"
    const val MINESTOM_EXTENSIONS = "dev.hollowcube:minestom-ce-extensions:1.2.0"

    const val KOTLIN_REFLECT = "org.jetbrains.kotlin:kotlin-reflect:1.9.23"
    const val KOTLINX_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0"
    const val KTOR_CLIENT_CORE = "io.ktor:ktor-client-core:2.3.10"
    const val KTOR_CLIENT_CIO = "io.ktor:ktor-client-cio:2.3.10"
    const val GSON = "com.google.code.gson:gson:2.10.1"
    const val REDISSON = "org.redisson:redisson:3.28.0"
    const val GUICE = "com.google.inject:guice:7.0.0"
    const val SSHD = "org.apache.sshd:sshd-sftp:2.12.1"
    const val JSCH = "com.jcraft:jsch:0.1.55"
    const val LOGBACK_CORE = "ch.qos.logback:logback-core:1.4.14"
    const val LOGBACK_CLASSIC = "ch.qos.logback:logback-classic:1.4.14"
    const val JAVALIN = "io.javalin:javalin:6.1.6"

    //Check compatibility with redisson
    const val NETTY_HANDLER = "io.netty:netty-handler:4.1.108.Final"
    const val NETTY_RESOLVER_DNS = "io.netty:netty-resolver-dns:4.1.108.Final"
    const val NETTY_RESOLVER = "io.netty:netty-resolver:4.1.108.Final"
    const val NETTY_TRANSPORT = "io.netty:netty-transport:4.1.108.Final"
    const val NETTY_BUFFER = "io.netty:netty-buffer:4.1.108.Final"
    const val NETTY_CODEC = "io.netty:netty-codec:4.1.108.Final"
    const val NETTY_COMMON = "io.netty:netty-common:4.1.108.Final"

    const val JLINE_CONSOLE = "org.jline:jline-console:3.25.1"
    const val JLINE_JANSI = "org.jline:jline-terminal-jansi:3.25.1"

    const val DOCKER_TEST_CONTAINERS = "org.testcontainers:testcontainers:1.19.7"
    const val BCPROV = "org.bouncycastle:bcprov-jdk15on:1.70"
    const val BCPKIX = "org.bouncycastle:bcpkix-jdk15on:1.70"
}

fun String.withVersion(version: String): String {
    return this.split(":").dropLast(1).joinToString(":") + ":$version"
}
