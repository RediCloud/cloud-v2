# Version / Updater

## Current version info

To display the current version information, the following command can be used:

`version`

This information is helpful in a bug report!



## Update your cloud (to latest)

1. Use `version checkupdate` to check if there is a new version
2. Download the version: `version download`
3. Switch to the version: `version switch`
4. Restart your cloud

{% hint style="info" %}
The `versions/` folder should be deleted regularly
{% endhint %}

##

## Update your cloud (to specific version)

1. You should also know the branch of the version first (show all branches: `version branches`)
2. Then you should check whether the build for this branch exists (show all builds: `version builds <branch>`)
3. Download the version: `version download <branch> <build>`
4. Switch to the version: `version switch <branch> <build<`
5. Restart your cloud

{% hint style="info" %}
The `versions/` folder should be deleted regularly
{% endhint %}
