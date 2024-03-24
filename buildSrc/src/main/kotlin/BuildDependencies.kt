object BuildDependencies {
    const val cloudVersion = "2.2.1-SNAPSHOT"
    const val cloudLibloaderVersion = "1.6.7"
    const val cloudLibloaderBootstrap = "dev.redicloud.libloader:libloader-bootstrap:1.6.7"

    const val spigotApi = "org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT"
    const val bungeeCordApi = "net.md-5:bungeecord-api:1.20-R0.3-SNAPSHOT"
    const val velocityApi = "com.velocitypowered:velocity-api:3.1.1"
    const val minestomApi = "dev.hollowcube:minestom-ce:1619cedc53"
    const val minestomExtensions = "dev.hollowcube:minestom-ce-extensions:1.2.0"

    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:1.8.22"
    const val gson = "com.google.code.gson:gson:2.10.1"
    const val kotlinxCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0"
    const val redisson = "org.redisson:redisson:3.27.1"
    const val khttp = "com.github.jkcclemens:khttp:0.1.0"
    const val guice = "com.google.inject:guice:7.0.0"
    const val sshd = "org.apache.sshd:sshd-sftp:2.10.0"
    const val jsch = "com.jcraft:jsch:0.1.55"
    const val logbackCore = "ch.qos.logback:logback-core:1.4.14"
    const val logbackClassic = "ch.qos.logback:logback-classic:1.4.14"

    //Check compatibility with redisson
    const val nettyHandler = "io.netty:netty-handler:4.1.107.Final"
    const val nettyResolverDns = "io.netty:netty-resolver-dns:4.1.107.Final"
    const val nettyResolver = "io.netty:netty-resolver:4.1.107.Final"
    const val nettyTransport = "io.netty:netty-transport:4.1.107.Final"
    const val nettyBuffer = "io.netty:netty-buffer:4.1.107.Final"
    const val nettyCodec = "io.netty:netty-codec:4.1.107.Final"
    const val nettyCommon = "io.netty:netty-common:4.1.107.Final"

    const val jlineConsole = "org.jline:jline-console:3.25.1"
    const val jlineJansi = "org.jline:jline-terminal-jansi:3.25.1"

    const val bcprov = "org.bouncycastle:bcprov-jdk15on:1.70"
    const val bcpkix = "org.bouncycastle:bcpkix-jdk15on:1.70"

    const val dockerTestContainers = "org.testcontainers:testcontainers:1.19.7"
}

fun String.withVersion(version: String): String {
    return this.split(":").dropLast(1).joinToString(":") + ":$version"
}