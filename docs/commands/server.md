# Server

## Delete a server

Only static servers can be deleted. You can do this with `server delete <server>`. The server folder is not deleted!



## Start server

To start a server, you can use the `server start <name> [count]` command. `count` is an optional parameter and does not have to be passed (standard count: 1).

But with static configuration templates this always starts a new server! To restart an already registered static server, you can use `server startstatic <name> <id>`. Example to start BuildServer-2: `server startstatic BuildServer 2`

##

## Stop server

Use the following command to stop a server:

`server stop <server> [force]`

Optionally, the boolean `true` or `false` can be passed to force-stop the server (kill the process).

Patterns like `server stop Lobby-*` can also be provided or simply separate several servers with a commar: `server stop Lobby-1,Lobby-4`



## Transfare server

A static server is always stored on a specific node. To change the node from a static server you can use the transfer command. Please note that the target node must be connected for this.

`server transfer <server> <node>`



## Other commands

* `ser list`
* `ser info <server>`
