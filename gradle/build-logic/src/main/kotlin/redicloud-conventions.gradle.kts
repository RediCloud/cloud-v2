plugins {
    kotlin("jvm")
    id("dev.redicloud.libloader")
    id("io.gitlab.arturbosch.detekt")
    `maven-publish`
}

val libs = the<VersionCatalogsExtension>().named("libs")

fun lib(name: String) = libs.findLibrary(name).get().get()

// Custom "dependency" configuration for libloader
// Wired into compile/runtime classpath and runtimeElements so dependencies are available
// at runtime and transitive to consuming projects (needed for IntelliJ direct-run)
val dependency by configurations.creating
configurations.compileClasspath.get().extendsFrom(dependency)
configurations.runtimeClasspath.get().extendsFrom(dependency)
configurations.named("runtimeElements") { extendsFrom(dependency) }

the(dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig::class).configurationName.set("dependency")
the(dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig::class).doBootstrapShade.set(false)

version = property("cloudVersion") as String

// --- Repositories ---
repositories {
    maven("https://maven.pkg.github.com/RediCloud/gradle-plugins") {
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.gradleProperty("gpr.key").orNull
                ?: providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
    maven("https://jitpack.io")
    mavenCentral()
}

// --- Dependencies ---
dependencies {
    compileOnly(lib("gson"))
    add("dependency", lib("libloader-bootstrap"))
    add("dependency", lib("kotlinx-coroutines"))
    (add("dependency", lib("ktor-client-cio")) as ModuleDependency)
        .exclude(group = "org.slf4j", module = "slf4j-api")
    (add("dependency", lib("ktor-client-core")) as ModuleDependency)
        .exclude(group = "org.slf4j", module = "slf4j-api")
    add("dependency", lib("kotlin-reflect"))
    add("dependency", lib("guice"))

    "detektPlugins"(lib("detekt-formatting"))
}

// --- Task configuration ---
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }

    withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "21"
        autoCorrect = true
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = true
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = "UTF-8"
    }

    // Expand ${version} placeholders in resource files (plugin.yml, bungee.yml, etc.)
    withType<ProcessResources> {
        val versionString = project.version.toString()
        filesMatching(listOf("plugin.yml", "bungee.yml", "extension.json", "module.json")) {
            filter { it.replace("\${version}", versionString) }
        }
    }

    // Generate redicloud-version.properties for local builds.
    // CI overwrites this file before the build, so this only provides dev defaults.
    val generateVersionProperties by registering {
        val outputDir = project.layout.buildDirectory.dir("generated/resources/version")
        val versionString = project.version.toString()
        val channel = providers.environmentVariable("REDICLOUD_CHANNEL").orNull ?: "dev"
        val buildNum = providers.environmentVariable("REDICLOUD_BUILD").orNull ?: "local"
        val gitSha = providers.environmentVariable("REDICLOUD_GIT").orNull
            ?: runCatching {
                Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=7", "HEAD"))
                    .inputStream.bufferedReader().readText().trim()
            }.getOrDefault("unknown")
        val branch = providers.environmentVariable("REDICLOUD_BRANCH").orNull ?: "dev"
        val fullVersion = if (channel == "stable") "$versionString+$gitSha"
            else "$versionString-$channel.$buildNum+$gitSha"

        inputs.property("version", versionString)
        inputs.property("channel", channel)
        inputs.property("build", buildNum)
        inputs.property("git", gitSha)
        outputs.dir(outputDir)

        doLast {
            val dir = outputDir.get().asFile
            dir.mkdirs()
            dir.resolve("redicloud-version.properties").writeText(
                listOf(
                    "version=$versionString",
                    "channel=$channel",
                    "build=$buildNum",
                    "git=$gitSha",
                    "branch=$branch",
                    "full-version=$fullVersion"
                ).joinToString("\n")
            )
        }
    }

    withType<ProcessResources> {
        from(generateVersionProperties)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    // Exclude signature files from shaded dependencies (e.g. Bouncy Castle)
    // to prevent "Invalid signature file digest" errors at runtime
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest {
        attributes["Main-Class"] = "dev.redicloud.libloader.boot.Bootstrap"
        attributes["Premain-Class"] = "dev.redicloud.libloader.boot.Agent"
        attributes["Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
        attributes["Launcher-Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
    }
    val v = if (project.version == "unspecified") project.parent?.version ?: "unknown" else project.version
    archiveFileName.set("redicloud-${project.name}-$v.jar")
}

// --- Publishing ---
afterEvaluate {
    val publishToRepository = runCatching {
        extra.get("publishToRepository").toString().toBoolean()
    }.getOrNull() ?: return@afterEvaluate
    if (!publishToRepository) return@afterEvaluate

    val repositoryUsername = providers.gradleProperty("gpr.user").orNull
        ?: providers.environmentVariable("GITHUB_ACTOR").orNull
    val repositoryPassword = providers.gradleProperty("gpr.key").orNull
        ?: providers.environmentVariable("GITHUB_TOKEN").orNull
    val repositorySlug = providers.environmentVariable("GITHUB_REPOSITORY").orNull
        ?: providers.environmentVariable("repository").orNull
    val repositoryUrl = providers.gradleProperty("gpr.url").orNull
        ?: (repositorySlug?.let { "https://maven.pkg.github.com/$it" }
            ?: "https://maven.pkg.github.com/RediCloud/cloud-v2")

    (extensions["publishing"] as PublishingExtension).apply {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri(repositoryUrl)
                credentials {
                    username = repositoryUsername
                    password = repositoryPassword
                }
            }
            publications {
                register<MavenPublication>("gpr") {
                    from(components["java"])
                }
            }
        }
    }
}
