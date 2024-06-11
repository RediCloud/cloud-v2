package dev.redicloud.utils


fun <T> Collection<T>.takeFirstLastRandom(count: Int): List<T> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<T>()
    result.add(first())
    if (size > 1) {
        result.add(last())
    }
    if (count - result.size <= 0) return result
    val randomEntries = drop(1).dropLast(1).shuffled().take(count - result.size)
    result.addAll(randomEntries)
    return result
}