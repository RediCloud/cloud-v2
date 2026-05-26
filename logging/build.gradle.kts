group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    dependency(libs.logback.core)
    dependency(libs.logback.classic)
}