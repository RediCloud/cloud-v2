# 🔧 Installation

## Requirements

* **Java**: 8 or higher (tested with 8, 11, 17, 21)
* Redis database

## Step-by-step

1. Download the cloud:
   * Stable build: [Download](https://api.redicloud.dev/v2/files/master/latest/redicloud.zip) (recommended)
   * Development build: [Download](https://api.redicloud.dev/v2/files/dev/latest/redicloud.zip)
   * CI (all builds): [https://github.com/RediCloud/cloud-v2/actions](https://github.com/RediCloud/cloud-v2/actions)
2. Unzip the redicloud.zip file (`unzip redicloud.zip`)
3. Set start-file permission (`chmod +x start.sh`)
4. Start the cloud (`./start.sh`)
5. Follow the database setup

{% hint style="warning" %}
You can currently only start node with java 9+. But it is then possible to switch between the Java versions for each individual server.
{% endhint %}
