group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    dependency("ch.qos.logback:logback-core:${Versions.logback}")
    dependency("ch.qos.logback:logback-classic:${Versions.logback}")
}