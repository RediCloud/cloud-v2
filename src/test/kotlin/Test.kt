import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile

fun main() {
    val container = GenericContainer("openjdk:17")
        .withCopyFileToContainer(MountableFile.forHostPath("F:/git/redicloud/cloud-v2/test-cloud/df3d/node-1"), "/data")
        .withCopyFileToContainer(MountableFile.forHostPath("F:/git/redicloud/cloud-v2/test-cloud/.libs"), "/libs")
        .withEnv("LIBRARY_FOLDER", "/libs")
        .withWorkingDirectory("/data")
        .withCommand("java", "-jar", "redicloud-node-service-2.2.1-SNAPSHOT.jar")
    container.start()
    while (true) {
        Thread.sleep(1000)
    }
}