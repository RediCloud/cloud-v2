import java.net.URI

plugins {
    `maven-publish`
}

group = "dev.redicloud.api"

val publishToRepository by extra(true)

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
}

publishing {
    repositories {
        maven {
            name = "redicloud"
            url = URI("https://repo.redicloud.dev/snapshots/")
            credentials(PasswordCredentials::class.java) {
                username = findProperty("REDI_CLOUD_REPO_USERNAME") as String?
                    ?: System.getenv("REDI_CLOUD_REPO_USERNAME")
                password = findProperty("REDI_CLOUD_REPO_PASSWORD") as String?
                    ?: System.getenv("REDI_CLOUD_REPO_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}