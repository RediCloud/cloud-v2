package dev.redicloud.utils

open class UtilTest {

    companion object {
        const val RANDOM_LOOP_TEST = 5
    }

    protected fun loopRandom(block: () -> Unit) {
        for (i in 0 until RANDOM_LOOP_TEST) {
            block()
        }
    }

    protected fun randomString(length: Int, specialChars: Boolean = false, notAllowedChars: List<Char> = emptyList()): String {
        val charPool : List<Char> = (('a'..'z') + ('A'..'Z') + ('0'..'9') +
                if (specialChars) "!@#$%^&*()_+".toList() else emptyList()).filter { !notAllowedChars.contains(it) }
        val randomString = (1..length)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
        return randomString
    }


}