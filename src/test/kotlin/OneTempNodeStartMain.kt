import cloud.RediCloud
import java.util.*

fun main() {
    val cloudName = "cloud-" + UUID.randomUUID().toString().substring(0, 4)
    println("Starting cloud $cloudName...")
    val cloud = RediCloud(
        cloudName,
        1,
        true,
    )
    while (true) {
        val input = readlnOrNull()
        if (input == "exit") {
            break
        }
    }
}