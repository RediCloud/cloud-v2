import java.security.MessageDigest
import java.util.UUID

val md5 = MessageDigest.getInstance("MD5")

fun String.toUUID(): UUID {
    val bytes = md5.digest(this.toByteArray())
    val bigInt = bytes.foldIndexed(0L) { index, acc, byte -> acc or ((byte.toLong() and 0xff) shl (index * 8)) }
    return UUID(bigInt, 0)
}

while (true) {
    println("Format: <type>_<version> (paper_1.20.4)")
    print("> ")
    val input = readlnOrNull() ?: break
    if (input == "exit") break
    println(input.toUUID())
}
