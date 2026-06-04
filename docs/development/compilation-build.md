# Compilation / Build

## Prerequisites

* **Java 21** (Temurin recommended)
* **Gradle 9.5+** (included via Gradle wrapper)

## Building

Clone the repository and build:

```bash
git clone https://github.com/RediCloud/cloud-v2.git
cd cloud-v2
./gradlew build
```

The build produces JAR files in each module's `build/libs/` directory. The main node service JAR is located at `services/node-service/build/libs/`.

## Project structure

The project uses a multi-module Gradle setup with a shared convention plugin (`gradle/build-logic/`). All subprojects apply the `redicloud-conventions` plugin, which configures:

* Kotlin JVM compilation targeting Java 21
* [Detekt](https://detekt.dev/) for static analysis
* [LibLoader](https://github.com/RediCloud/libloader) for runtime dependency loading
* `maven-publish` for GitHub Packages publishing
* JAR naming: `redicloud-<module>-<version>.jar`
* Version properties generation (`redicloud-version.properties`)

### Key modules

| Module | Description |
|--------|-------------|
| `core` | Unified module for database, packets, events, and cache |
| `utils` | Shared utilities (CloudVersion, HashUtils, caching, coroutines) |
| `console` | Cloud console with JLine terminal |
| `logging` | Logging configuration |
| `tasks` | Task scheduling framework |
| `repositories` | Data repositories (node, server, player, config templates, etc.) |
| `migrations` | Migration framework for database and local file migrations |
| `updater` | Cloud self-updater with GPG verification |
| `file-cluster` | SFTP-based clustered file system and templates |
| `apis/base-api` | Base API interfaces |
| `apis/connector-api` | Connector API for plugin development |
| `apis/node-api` | Node API |
| `services/node-service` | Node service (main entry point) |
| `services/base-service` | Shared service layer |
| `services/minecraft-server-service` | Minecraft server service |
| `services/proxy-server-service` | Proxy server service |
| `connectors/*` | Platform connectors (Bukkit, Velocity, BungeeCord, Minestom) |
| `modules/*` | Module system and built-in modules |
| `commands/command-api` | Command framework |
| `test-framework` | Test framework for plugin development |

## Version catalog

Dependencies are managed via `gradle/libs.versions.toml`. To add or update a dependency, edit this file.

## Static analysis

Detekt is configured via `detekt.yml` in the project root. Run it with:

```bash
./gradlew detekt
```

