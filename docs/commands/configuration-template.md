---
description: >-
  A configuration template is what is usually known as a group in other clouds.
  You can find everything important there, such as: Set the max memory of a
  server.
---

# Configuration template



## Stop time

Set the time after a server should be stopped if it is useless (no connected players, more server connected than needed...).

```
ct edit <name> stoptime <minutes>
```



## Max players

Set the max players for a configuration template.

```
ct edit <name> maxplayers <count>
```

{% hint style="info" %}
Some versions such as minestom does not support this by default. You can implementate your [custom player provided](../development/api/custom-player-provider.md) on the server.
{% endhint %}



## File edits

File edits are very helpful for making basic server settings. E.g. With paper you could set the port directly in the server.properties. This makes it possible to provide simple support for other software without programming anything. Example of the command for the type paper to set the port:

`ct edit bedwars8x1 fe add bedwars.properties size 8x1`

You can remove edits as follows:

`ct edit <type> fe remove <file> <key>`

Following file types are supported: .toml, .yml, .properties

To use hierarchical keys, simply connect the individual keys with a dot: `server-info.motd.first-line`

##

## Template files

These are then downloaded into the server folder.

You can add these with `ct edit <version> files add <url> [path]` and remove them with `ct edit <version> files remove <url>`

Example: `ct edit bedwars files add https://link.to/bedwars.config config/bedwars.config`

{% hint style="warning" %}
Currently not implemented for configuration templates
{% endhint %}



## Program parameters

You can start a server version with specific program arguments. You can set this up as follows:

`ct edit <version> programargument <argument>`

`ct edit <version> programargument <argument>`



## JVM argumentes&#x20;

To use specific java arguments you can use the following commands:

`ct edit <version> jvmarg add <argument>`

`ct edit <version> jvmarg remove <argument>`



## Environments

It is also possible to set environment variables:

`ct edit <name> environment add <key> <value>`

`ct edit <name> environment remove <key>`



## Max memory

You can set the max memory of a server as follows:

`ct edit <name> maxmemory <memory>`

The memory should be provided in MB.



## File templates

You can specify which file templates should be used by a configuration template as follows:&#x20;

`ct edit <name> ft add <template>`

`ct edit <name> ft remove <template>`

The files from the template are copied into the server directory when the server starts

{% content-ref url="file-templates.md" %}
[file-templates.md](file-templates.md)
{% endcontent-ref %}

## Start node

You can specify on which nodes a server of a configuration template can be started as follows:

`ct edit <name> node add <node>`

`ct edit <name> node remove <node>`

If a server is allowed to start on all nodes, remove all start nodes from the template.



## Min services

With the following command you can specify how many servers must at least be started **globally**:

`ct edit <name> minServices <count>`

To set the minimum number of servers **per node** that should be started, you can use the following command:

`ct edit <name> minServicesPerNode <count>`



## Max services

With the following command you can specify the maximum number of servers that must be started **globally**:

`ct edit <name> maxServices <count>`

To set the maximum number of startups **per node**, you can use the following command:

`ct edit <name> maxServicesPerNode <count>`



## Auto-start at percentage of players

It is often helpful to automatically start servers with a certain number of players. If e.g. if 7 players have already joined an 8x1 game server, it makes sense to start a nine server. To set the percentage fullness of the server, when a new one should be started, you can use the following command:

`ct edit <name> percentToStartNew <percent>`

{% hint style="warning" %}
Currently not implemented
{% endhint %}



## Server splitter

You can also customize the separator between name and id (default: name-id) with the following command:

`ct edit <name> splitter <value>`



## Fallback

A fallback server is a server where players land on when they enter the network or are kicked from another subserver. To set this you can use the following command:

`ct edit <name> fallback <true/false>`



## Start priority

It can happen that many servers are added to the queue at once. It is possible to prioritize the start of servers. You can do this with the following command:

`ct edit <name> startPriority <priority>`

Servers with a priority of 0 are started first and those with a priority of 100 are started last.



## Server version&#x20;

A configuration template must be set to a [server version](configuration-template.md#server-version). You can do this as follows:

`ct edit <name> version <version>`



## Start port

A start port on which the server should be started can be set as follows:

`ct edit <name> startPort <port>`

The first server starts on the corresponding start port. Each additional server is assigned the next free port after the start port. It is automatically checked whether the port is free.



## Static

If a server is static, the server folder will not be deleted after stopping. The server folder is located in the `storage/static` folder. A non-static server (dynamic) is deleted after the stop. They are located in the `/tmp` folder.

`ct edit <name> static <true/false>`



## Join permission

If a player needs a specific permission to enter the server, you can set it as follows:

`ct edit <name> permission <permission>`

If no permission is required, set it to `null` .



## Other commands

* `ct create <name>`
* `ct delete <name>`
* `ct edit <name> name <new-name>`
* `ct list`
* `ct info <name>`

