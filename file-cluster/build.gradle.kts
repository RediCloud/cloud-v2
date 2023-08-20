group = "dev.redicloud"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":cache"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":repositories:cache-repository"))

    dependency("org.apache.sshd:sshd-sftp:2.10.0")
    dependency("com.jcraft:jsch:0.1.55")
    dependency("org.bouncycastle:bcprov-jdk15on:1.70")
    dependency("org.bouncycastle:bcpkix-jdk15on:1.70")
}