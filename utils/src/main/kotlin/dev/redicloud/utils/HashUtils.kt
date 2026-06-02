package dev.redicloud.utils

import java.io.File
import java.security.MessageDigest

private const val HASH_BUFFER_SIZE = 8192

/**
 * Computes the SHA-256 hash of a file.
 *
 * @return lowercase hex-encoded SHA-256 hash.
 */
fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(HASH_BUFFER_SIZE)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * Verifies a file's SHA-256 against an expected hash string.
 *
 * @return `true` if the hash matches (case-insensitive), `false` otherwise.
 */
fun verifyFileHash(file: File, expectedSha256: String): Boolean {
    val actualHash = sha256(file)
    return actualHash.equals(expectedSha256, ignoreCase = true)
}
