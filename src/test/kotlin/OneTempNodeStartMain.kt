import cloud.RediCloud
import java.util.*

fun main() {
    val cloudName = UUID.randomUUID().toString().substring(0, 4)
    val cloud = RediCloud(
        cloudName,
        1,
        true,
        "2.2.1-SNAPSHOT"
    )
    while (true) {
        val input = readlnOrNull()
        if (input == "exit") {
            cloud.stop()
            break
        }
    }
}