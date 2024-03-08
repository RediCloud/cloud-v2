# Java versions

It is possible to set a separate Java version for each server version. The cloud supports java versions between 8 & 21.



## Auto locate

Mit `jv auto-locate` werden automatisch alle installierten java version aus folgenden verzechnissen geladen und als server version erstellt:

**Windows**:

* Program Files/Java
* Program Files (x86)/Java

**Linux:**

* /usr/lib/jvm
* /usr/lib64/jvm

If you use your own installation directory, you can set the property `redicloud.java.versions.path`. If there is a `JAVA_HOME` environment variable, this directory will also be scanned.



## Locate manually

To locate a java version manually you can use `jv locate <name> <path>`. This is helpful if you need to localize an existing Java version on another node.

Example: `jv locate java-17 /usr/lib/jvm/java17`



## Other commands

* `jv list`
* `jv delete`
