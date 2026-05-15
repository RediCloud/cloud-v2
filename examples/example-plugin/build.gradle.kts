group = "dev.redicloud.example.plugin"

repositories {
    /*
    Github redicloud packages:

    maven("https://maven.pkg.github.com/RediCloud/cloud-v2") {
        credentials {
            username = project.findProperty("github_username") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("github_token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
     */

    /*
    Maven users can use the same repository in pom.xml:

    <repositories>
        <repository>
            <id>redicloud-github</id>
            <url>https://maven.pkg.github.com/RediCloud/cloud-v2</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>dev.redicloud.api</groupId>
        <artifactId>base-api</artifactId>
        <version>${redicloud.version}</version>
        <scope>provided</scope>
    </dependency>
     */

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
}

dependencies {

    /*
    Use the code below to add the cloud api:

    compileOnly("dev.redicloud.api:base-api:<cloud-version>")
     */

    compileOnly(BuildDependencies.SPIGOT_API)

    // Internal usage, ignore it! You don't need to add this
    compileOnly(project(":apis:base-api"))
    compileOnly(BuildDependencies.KYORI_ADVENTURE_API)
}
