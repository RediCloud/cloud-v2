group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    dependency(BuildDependencies.logbackCore)
    dependency(BuildDependencies.logbackClassic)
}