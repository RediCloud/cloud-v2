# RediCloud

A redis based cluster cloud system for minecraft. 
**[[Discord](https://discord.gg/g2HV52VV4G)] [[Wiki](https://docs.redicloud.dev)]**
<br>

### Overview

- [Features](#features)


### Features

! The cloud is still in development. Not all planned features are listed. If you are missing a feature or have an idea, join the RediCloud discord. !

* redis for communication and storage
* node clustering (decentralized)
* start minecraft services ([spigot](https://getbukkit.org/download/spigot) , [bukkit](https://getbukkit.org/download/craftbukkit), [paper](https://papermc.io/) based forks, [minestom-ce](https://github.com/hollow-cube/minestom-ce))
* start proxy services ([bungeecord](https://www.spigotmc.org/wiki/bungeecord/) , [waterfall](https://github.com/PaperMC/Waterfall), [velocity](https://github.com/PaperMC/Velocity))
* minestom impl also usable
* version handler api (auto update)
* online files / version (live update without cloud restart)
* console + commands
* dynamic and static services
* templates (clustered via sftp)
* api for developer (modules/plugins/connectors)
* mc version: 1.8-latest support
* custom server versions (like custom paper)
* auto java version detection
* multi java versions support (versions: 8-21)
* modify program arguments and jvm flags for each group / server version
* easy dev plugin test -> [test-framework](https://docs.redicloud.dev/development/test-framework)
* module system
* report server crashes
* log server exceptions to node
* suspend system (suspend time outed nodes...)
* server version support (auto update, pre versions...)
* static server transfer to other nodes
* smart server start (retry other nodes on failed start)
* print server error directly to node console (loop, spam protection)
* clean console design
* Small further details (designs, small functions) can be changed per property, no code editing needed!
* [updater](https://docs.redicloud.dev/commands/version-updater) command
* ...