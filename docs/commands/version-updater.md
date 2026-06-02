# Version / Updater

## Current version info

To display the current version information, the following command can be used:

`version`

This information is helpful in a bug report!

## Upgrade your cloud (to latest)

1. Use `version checkupdate` to check if there is a new version
2. Upgrade: `version upgrade`
3. Shut down the entire cluster
4. Restart all nodes — migrations will run automatically on first startup

The `version upgrade` command will upgrade **all connected nodes** in the cluster automatically.
A live status overview shows the progress per node during the upgrade.

{% hint style="info" %}
The `versions/` folder should be deleted regularly
{% endhint %}

## Upgrade your cloud (to specific version)

1. Check available channels: `version channels`
2. Check available releases for a channel: `version releases <channel>`
3. Upgrade: `version upgrade <channel> <version>`
4. Shut down the entire cluster
5. Restart all nodes — migrations will run automatically on first startup

{% hint style="info" %}
The `versions/` folder should be deleted regularly
{% endhint %}

## Migrations

When the cloud version changes, database and local file migrations run automatically on startup:

- **Database migrations** run once per cluster (protected by a distributed lock). Other nodes wait until the migration is complete.
- **Local migrations** run on every node independently.

The schema version is stored in Redis at `cloud:schema-version`.

## Auto-upgrade on version mismatch

If a node starts with a JAR version that is older than the cluster's schema version,
it will automatically download and install the correct version, then shut down.
On the next restart, the node will start with the correct version and run any pending migrations.
