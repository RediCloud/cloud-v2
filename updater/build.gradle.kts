group = "dev.redicloud"

dependencies {
    shade(project(":apis:base-api"))
    shade(project(":logging"))
    shade(project(":utils"))
    shade(project(":tasks"))
}