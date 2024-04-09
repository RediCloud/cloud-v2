package dev.redicloud.testing.pre

enum class PreJavaVersion(
    val versionName: String,
    val alpinPackage: String
) {

    JAVA_8("java-1.8-openjdk", "openjdk8"),
    JAVA_11("java-11-openjdk", "openjdk11"),
    JAVA_17("java-17-openjdk", "openjdk17"),
    JAVA_21("java-21-openjdk", "openjdk21")

}