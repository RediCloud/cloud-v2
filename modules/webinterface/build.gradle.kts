plugins {
    id("io.ktor.plugin") version "2.3.10"
}

application {
    mainClass.set("dev.redicloud.module.webinterface.WebinterfaceModuleKt")
}

group = "dev.redicloud.modules"

dependencies {
    implementation(project(":apis:base-api"))
    implementation(project(":utils"))
    implementation(project(":logging"))
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-sessions-jvm")
    implementation("io.ktor:ktor-server-http-redirect-jvm")
    implementation("io.ktor:ktor-server-freemarker-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-gson-jvm")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation(BuildDependencies.LOGBACK_CLASSIC)

    testImplementation(project(":test-framework"))
}
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<JavaCompile> {
        options.release.set(17)
    }
}

tasks.register("reloadTemplates") {
    group = "other"
    description = "Reload all html/css templates on runtime"
    dependsOn(tasks.named("processResources"))
}