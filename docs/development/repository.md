# Repository

## Github packages:

```gradle
// build.gradle.kts
maven("https://maven.pkg.github.com/RediCloud/cloud-v2") {
    credentials {
        username = project.findProperty("github_username") ?: System.getenv("GITHUB_USERNAME")
        password = project.findProperty("github_token") ?: System.getenv("GITHUB_TOKEN")
    }
}
```

The following packages are currently included on [github packages](https://github.com/orgs/RediCloud/packages?repo\_name=cloud-v2):

* [dev.redicloud.api:base-api](https://github.com/RediCloud/cloud-v2/packages/2104921)
* [dev.redicloud.api:connector-api](https://github.com/RediCloud/cloud-v2/packages/2104922)
* [dev.redicloud.api:node-api](https://github.com/RediCloud/cloud-v2/packages/2104923)
* [dev.redicloud:test-framework](https://github.com/RediCloud/cloud-v2/packages/2118336)
* [dev.redicloud:console](https://github.com/RediCloud/cloud-v2/packages/2104917)
* [dev.redicloud:logging](https://github.com/RediCloud/cloud-v2/packages/2104918)
* [dev.redicloud:tasks](https://github.com/RediCloud/cloud-v2/packages/2104919)
* [dev.redicloud:utils](https://github.com/RediCloud/cloud-v2/packages/2104920)

More information about github packages:&#x20;

* Gradle: [https://docs.github.com/de/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry](https://docs.github.com/de/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
* Maven: [https://docs.github.com/de/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry](https://docs.github.com/de/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

## Create your own personal access token:

You can create your token here: [https://github.com/settings/tokens?type=beta](https://github.com/settings/tokens?type=beta)

(Read-only permission should be enough access github packages)

More information: [https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
