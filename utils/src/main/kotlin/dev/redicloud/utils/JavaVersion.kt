package dev.redicloud.utils

enum class JavaVersion(val version: String, val id: Int) {
    JAVA_1_0("1.0", 45),
    JAVA_1_2("1.2", 46),
    JAVA_1_3("1.3", 47),
    JAVA_1_4("1.4", 48),
    JAVA_5("5", 49),
    JAVA_6("6", 50),
    JAVA_7("7", 51),
    JAVA_8("8", 52),
    JAVA_9("9", 53),
    JAVA_10("10", 54),
    JAVA_11("11", 55),
    JAVA_12("12", 56),
    JAVA_13("13", 57),
    JAVA_14("14", 58),
    JAVA_15("15", 59),
    JAVA_16("16", 60),
    JAVA_17("17", 61),
    JAVA_18("18", 62),
    JAVA_19("19", 63),
    JAVA_20("20", 64),
    UNKNOWN("unknown", -1)
}

fun getJavaVersion(): JavaVersion {
    val version = System.getProperty("java.version")
    val versionParts = version.split(".")
    val major = versionParts[0].toInt()
    val minor = versionParts[1].toInt()
    return JavaVersion.values().find { it.version == major.toString() } ?: JavaVersion.UNKNOWN
}

val supportedJavaVersions = mutableListOf(JavaVersion.JAVA_8, JavaVersion.JAVA_17, JavaVersion.JAVA_19, JavaVersion.JAVA_18)
val notTestedJavaVersions = mutableListOf(JavaVersion.JAVA_9, JavaVersion.JAVA_10, JavaVersion.JAVA_12, JavaVersion.JAVA_13, JavaVersion.JAVA_14, JavaVersion.JAVA_15, JavaVersion.JAVA_13, JavaVersion.JAVA_16, JavaVersion.JAVA_18, JavaVersion.JAVA_19, JavaVersion.JAVA_20)
val incompatibleJavaVersions = JavaVersion.values().filter { !supportedJavaVersions.contains(it) && !notTestedJavaVersions.contains(it) || it == JavaVersion.UNKNOWN}