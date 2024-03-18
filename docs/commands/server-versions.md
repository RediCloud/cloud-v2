# Server versions

The server version is the software from the server. For example paper 1.20.4. It is possible to customize this server version as desired.

The main components of a server version are the server type (software like paper, spigot...) and the software versions (usually the actual mc version e.g. 1.20.4, with velocity e.g. 3.3.0-SNAPSHOT).

There are already a wide number of server versions that are created automatically. This includes paper, velocity, spigot, bungeecord and waterfall, each with all versions from 1.8 to the latest version



## Create your own server version

Do you have your own fork of paper? You can also use this very easily.

You can create your own version with `sv create <project> <version>`. Project is the name such as MyPaper and version is e.g. 1.8.8. Then you have to set the software type and the download link.



## Download url

You can set a version handler to a server version. This handler is there for downloading & updating the version. By default there is the simple url downloader. This only downloads the version again from the specified URL if it is not available in`storage/versions`. With `sv edit <version> url <url>` you can set the download url (Additional handlers can be added via modules).&#x20;

Further information on how to create your own version handler can be found [HERE](https://docs.redicloud.dev/commands/server-version-types#version-handler). You can find out how to assign this to a version [HERE](https://docs.redicloud.dev/development/api/server-version-handlers).

{% content-ref url="server-version-types.md" %}
[server-version-types.md](server-version-types.md)
{% endcontent-ref %}



## Set the type

You can set the type with `sv edit <version> type <type>`. This requires which interface should be used between the cloud & server (connector).



## Download manually

The download can be started manually with `sv download <version>`. If a server version local is not available, it will be downloaded automatically.



## Version files

Version files that a server version requires can also be specified. These are then downloaded into the version directory (`storage/versions`) and copied into the server folder with the server.jar.

You can add these with `sv edit <version> files add <url> [path]` and remove them with `sv edit <version> files remove <url>`

Example: `sv edit paper_1.20.4 files add https://link.to/version.config config/version.config`



## Program parameters

You can start a server version with specific program arguments. You can set this up as follows:

`sv edit <version> programargument <argument>`

`sv edit <version> programargument <argument>`

##

## JVM argumentes&#x20;

To use specific java arguments you can use the following commands:

`sv edit <version> jvmarg add <argument>`

`sv edit <version> jvmarg remove <argument>`



## Patching

Patching a server version can often be helpful. Such as for paper. Libs are downloaded there when the server is started for the first time. So that the download doesn't have to be done again every time, you can activate patching and thus significantly improve the start time of your server. You can activate this with the following command: `sv edit <version> patch true`

Which files should be stored in the `storage/versions/` folder after the patching process. You can set libpattern `with`sv edit . This pattern is a normal java regex (this can be tested here [regex100](https://regex101.com/)). For paper, for example, this would be `(cache|versions|libraries)`.



## More commands

* `sv list`
* `sv delete <name>`
* `sv duplicate <name> [new-name]`
