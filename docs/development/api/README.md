# API

The API modules are published as Maven artifacts through GitHub Packages.

Gradle:

```kotlin
repositories {
    maven("https://maven.pkg.github.com/RediCloud/cloud-v2") {
        credentials {
            username = project.findProperty("github_username") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("github_token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("dev.redicloud.api:base-api:<cloud-version>")
}
```

Maven:

```xml
<repositories>
    <repository>
        <id>redicloud-github</id>
        <url>https://maven.pkg.github.com/RediCloud/cloud-v2</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.redicloud.api</groupId>
        <artifactId>base-api</artifactId>
        <version>${redicloud.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>net.kyori</groupId>
        <artifactId>adventure-api</artifactId>
        <version>4.17.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Available API artifacts:

- `dev.redicloud.api:base-api`
- `dev.redicloud.api:node-api`
- `dev.redicloud.api:connector-api`

