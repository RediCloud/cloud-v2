# RediCloud

A Redis-based, decentralized cluster cloud system for Minecraft.
**[[Discord](https://discord.gg/g2HV52VV4G)] [[Wiki](https://docs.redicloud.dev)] [[Releases](https://github.com/RediCloud/cloud-v2/releases)]**

### Overview

- [Features](#features)
- [Installation](#installation)

### Features

* Redis for communication and storage
* Node clustering (decentralized)
* Start Minecraft services ([Spigot](https://getbukkit.org/download/spigot), [Bukkit](https://getbukkit.org/download/craftbukkit), [Paper](https://papermc.io/) based forks, [Minestom](https://github.com/hollow-cube/minestom-ce))
* Start proxy services ([BungeeCord](https://www.spigotmc.org/wiki/bungeecord/), [Waterfall](https://github.com/PaperMC/Waterfall), [Velocity](https://github.com/PaperMC/Velocity))
* Minestom implementation also usable
* Version handler API (auto update)
* Online files / versions (live update without cloud restart)
* Console + commands
* Dynamic and static services
* Templates (clustered via SFTP)
* API for developers (modules/plugins/connectors)
* MC version: 1.8-latest support
* Custom server versions (like custom Paper)
* Auto Java version detection
* Multi Java versions support (versions: 8-21)
* Modify program arguments and JVM flags for each group / server version
* Easy dev plugin testing -> [test-framework](https://docs.redicloud.dev/development/test-framework)
* Module system
* Report server crashes
* Log server exceptions to node
* Suspend system (suspend timed-out nodes)
* Server version support (auto update, pre-release versions)
* Static server transfer to other nodes
* Smart server start (retry other nodes on failed start)
* Print server errors directly to node console (loop & spam protection)
* Clean console design
* Small details (designs, small functions) can be changed per property, no code editing needed
* [Updater](https://docs.redicloud.dev/commands/version-updater) command with cluster-wide upgrade support
* Migration framework for automatic database and file migrations on version upgrades
* GPG signature and SHA-256 checksum verification for all downloads
* Connector downloads via GitHub Releases with signed manifest verification
* Consolidated start script (`start.sh`) with debug, screen, and verbose flags

### Installation

```bash
curl -sSL https://raw.githubusercontent.com/RediCloud/cloud-v2/main/install.sh | bash
```

Or download the latest release manually from [GitHub Releases](https://github.com/RediCloud/cloud-v2/releases).

See the [installation guide](https://docs.redicloud.dev/installation) for details.