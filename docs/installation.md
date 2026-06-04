# Installation

## Requirements

* **Java**: 21 or higher
* Redis database ([Introduction](https://www.digitalocean.com/community/tutorials/how-to-install-and-secure-redis-on-debian-10))
* `curl`, `unzip` (for the install script)
* `gpg` (optional, for signature verification during installation)

## Automated installation (recommended)

The easiest way to install RediCloud is using the install script:

```bash
curl -sSL https://raw.githubusercontent.com/RediCloud/cloud-v2/main/install.sh | bash
```

### Install script options

| Flag | Description | Default |
|------|-------------|---------|
| `--channel <name>` | Release channel: `stable` or `beta` | `stable` |
| `--version <ver>` | Specific version (e.g. `2.5.0`, `2.5.0-beta.3`) | `latest` |
| `--dir <path>` | Installation directory | `.` (current dir) |
| `--no-verify` | Skip SHA-256 checksum verification | verify enabled |
| `-h, --help` | Show help | -- |

Example:

```bash
# Install latest stable release into ./cloud
bash install.sh --channel stable --dir ./cloud

# Install a specific beta version
bash install.sh --channel beta --version 2.5.0-beta.1
```

The installer automatically verifies downloads using **SHA-256 checksums** from a signed `manifest.json`. If `gpg` is installed, it additionally verifies the **GPG signature** of the manifest.

## Manual installation

1. Download the latest release from [GitHub Releases](https://github.com/RediCloud/cloud-v2/releases)
2. Unzip the redicloud.zip file (`unzip redicloud.zip`)
3. Set start-file permission (`chmod +x start.sh`)
4. Start the cloud (`./start.sh`)
5. Follow the database setup

## Start script

The `start.sh` script starts the RediCloud node. It supports the following flags:

| Flag | Description | Default |
|------|-------------|---------|
| `--screen` | Run inside a GNU screen session | `false` |
| `--debug` | Enable JDWP remote debugger | `false` |
| `--port <port>` | Set debugger port (implies `--debug`) | `5005` |
| `--verbose` | Set log level to `FINEST` | `false` |
| `--no-ping` | Disable node-ping task (for local development) | `false` |
| `-h, --help` | Show help | -- |

Example:

```bash
# Normal start
./start.sh

# Start in a screen session
./start.sh --screen

# Start with debug enabled on port 5005
./start.sh --debug
```

{% hint style="warning" %}
The node requires Java 21+ to start. It is possible to configure different Java versions (8-21) for individual server instances.
{% endhint %}
