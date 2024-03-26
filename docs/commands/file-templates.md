# File templates

A file template is a folder that can be found under `storage/templates`. If the template is used by a configuration-template, all files from the folder are copied to the server folder. A template has a prefix & a name. This could e.g. bedwars/8x1 (bedwars = prefix, 8x1 = name).

Format: `prefix-name`



## Inheritances

Inheritances are useful in the following cases: You have a waiting lobby for bedwars. But you don't want to load these individually into each bedwars template. You create a template bedwars/base. To the other templates `bedwars-8x1`, `bedwars-2x1` etc. you simply add it as an inheritance:

`ft edit <name> inherit add <inherit-template>`

`ft edit <name> inherit remove <inherit-template>`



## Publication

With this command you can send all templates to a specific node.

`ft publish node02`



## Other commands

* `ft info <name>`
* `ft list`
* `ft create <name> <prefix>`
* `ft edit <name> name <new-name>`
* `ft edit <name> prefix <new-prefix>`
