group = "dev.redicloud.connector.bungeecord"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":database"))
    compileOnly(project(":utils"))
    compileOnly(project(":services:minecraft-server-service"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":services:proxy-server-service"))
    compileOnly(project(":repositories:player-repository"))
    compileOnly(project(":repositories:server-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":repositories:server-version-repository"))

    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
}