# Cluster

If you connect several nodes together, this is called a cluster.

## Connect a node to a cluster

Just do the normal node setup, like with the first node. Simply specify the same database as from the first node. This will automatically connect the nodes.

## Cluster-wide upgrades

When you run `version upgrade`, **all connected nodes** in the cluster are upgraded automatically. The upgrade orchestrator coordinates the process and shows a live status overview with the progress per node. After the upgrade, shut down the entire cluster and restart all nodes -- migrations will run automatically on first startup.

See [Version / Updater](version-updater.md) for details.

## File template publication

See [File-template/Publication](file-templates.md#publication) (does the same).

`cluster publish <node>`

## Suspend node

If a node is not running properly you can suspend it. This should stop this node automatically.

`cluster suspend <node>`

## Node max memory

The maximum RAM available to the cloud servers of a node can be set as follows (in MB):

`cluster edit <node> maxmemory <value>`

## Other commands

* `cluster list`
* `cluster ping <node>`
