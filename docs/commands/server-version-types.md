# Server version types

The server version type is part of a server version. This represents the type of software. So e.g. paper, velocity, bukkit etc

## Create a version type

To create a new type you can simply use `svt create <name>`. You can then edit with `svt edit <name>`.



## File edits

File edits are very helpful for making basic server settings. E.g. With paper you could set the port directly in the server.properties. This makes it possible to provide simple support for other software without programming anything. Example of the command for the type paper to set the port:

`svt edit paper fe add server.properties server-port %PORT%`

You can remove edits as follows:

`svt edit <type> fe remove <file> <key>`

Following file types are supported: .toml, .yml, .properties

To use hierarchical keys, simply connect the individual keys with a dot: `server-info.motd.first-line`



## Version handler

A version handler is there to download the server versions and patch them if necessary. The version handler, which is implemented by default in the cloud, is the URL downloader. This downloads the server version from the specified URL and also offers the option to patch it. Through other implementations of the versions handler, it would also be possible to download new builds automatically. An example would be the papermc module. This provides the interface to the papermc.io API and always updates to the latest builds. You can set the version handler of a version as follows:&#x20;

`svt edit <type> handler <handler>`&#x20;

With `svt handlers` you will see all registered version handlers.



## Connector settings

The connector represents the interface between the cloud and the server. E.g. in the case of paper in the form of a plugin. It is also possible to use your own connectors, which you have implemented yourself. To do this, change the connector download url to your own with `svt edit <name> connector url <url>`.&#x20;

With `svt edit <name> connector file <filename>` you can set the name under which the connector file is saved.

Depending on the server software, there is always a different folder on the server for plugins. As an example, in minestom it is the extensions folder. To set this you can use `svt edit <name> connector folder <folder>`.



## Version type files

Files that a server version type requires can also be specified. These are then downloaded into the version directory (`storage/versions`) and copied into the server folder with the server.jar.

You can add these with `svt edit <version> files add <url> [path]` and remove them with `svt edit <version> files remove <url>`

Example: `svt edit paper files add https://link.to/paper.config config/paper.config`



## Proxy

With `svt edit <name> proxy <true/false>` you have to set whether it is a proxy or a subserver.



## Program parameters

You can start a server version with specific program arguments. You can set this up as follows:

`sv edit <version> programargument <argument>`

`sv edit <version> programargument <argument>`

##

## JVM argumentes&#x20;

To use specific java arguments you can use the following commands:

`sv edit <version> jvmarg add <argument>`

`sv edit <version> jvmarg remove <argument>`



## More commands

* `svt edit <name> name <new-name>`
* `svt list`
* `svt info <name>`
* `svt delete <name>`
