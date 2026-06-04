# Repository

## GitHub Packages

```kotlin
// build.gradle.kts
maven("https://maven.pkg.github.com/RediCloud/cloud-v2") {
    credentials {
        username = project.findProperty("github_username") as String? ?: System.getenv("GITHUB_USERNAME")
        password = project.findProperty("github_token") as String? ?: System.getenv("GITHUB_TOKEN")
    }
}
```

The following packages are published to [GitHub Packages](https://github.com/orgs/RediCloud/packages?repo_name=cloud-v2):

* `dev.redicloud.api:base-api` -- base API interfaces (ICloudConsole, ICloudTask, ICloudTaskManager, ProcessConfiguration, etc.)
* `dev.redicloud.api:connector-api` -- connector API for plugin/extension development
* `dev.redicloud.api:node-api` -- node API
* `dev.redicloud:test-framework` -- test framework for plugin development
* `dev.redicloud:utils` -- shared utilities (CloudVersion, HashUtils, caching, coroutines, etc.)
* `dev.redicloud:logging` -- logging configuration

More information about GitHub Packages:

* Gradle: [https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
* Maven: [https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

## Create your own personal access token

You can create your token here: [https://github.com/settings/tokens?type=beta](https://github.com/settings/tokens?type=beta)

Read-only permission should be enough to access GitHub Packages.

More information: [https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
