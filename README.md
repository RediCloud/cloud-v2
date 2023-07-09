# RediCloud (WIP = work in progress)

A redis based cluster cloud system for minecraft. 
**[[Discord](https://discord.gg/g2HV52VV4G)]**
<br>

### Overview

- [Features](#features)


### Features

(✅ = done | ⚙️ = in progress | ❌ = not started | 🚧 = done, but not tested enough)

! The cloud is still in early development. Not all planned features are listed. If you are missing a feature or have an idea, join the RediCloud discord. !

- redis for communication and storage
- node clustering (decentralized)
- start minecraft services ([spigot](https://getbukkit.org/download/spigot)
  , [bukkit](https://getbukkit.org/download/craftbukkit), [paper](https://papermc.io) based forks)
- start proxy services ([bungeecord](https://www.spigotmc.org/wiki/bungeecord/)
  , [waterfall](https://github.com/PaperMC/Waterfall), [velocity](https://github.com/PaperMC/Velocity))
- minestom impl also usable
- version handler api (auto update)
- mods support
- online files / version (live update without cloud restart)
- gui to manage the cloud
- console + commands
- dynamic and static services
- templates (clustered)
- api for developer (modules/plugins)
- mc version: 1.8-latest support
- custom server versions (like custom paper)
- auto java version detection
- mutli java versions support (versions: 8-20)
- modify programm arguments and jvm flags for each group
- external proxy server (start external proxy server and connect them to the cloud)
- easy dev plugin test (create external server, that you can start for e.g via your IDE. The services will connect without a node to the cloud cluster)
- offline/online player support at the same time
- multi proxy (with player count sync)
- limbo fallbacks
- only proxy join (but please use your firewall: [guide](https://www.spigotmc.org/wiki/firewall-guide/))
- module system
- report server crashes
- log server exceptions to node
- suspend system (suspend time outed nodes...)
- server version support (auto update, pre versions...)
- static server transfer to other nodes
- smart server start (retry other nodes on failed start)
- print server error directly to node console (loop, spam protection)
- clean console design
- Small further details (designs, small functions) can be changed per property, no code editing needed!
- ...
