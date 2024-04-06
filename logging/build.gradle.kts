group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    dependency(BuildDependencies.LOGBACK_CORE)
    dependency(BuildDependencies.LOGBACK_CLASSIC)
}