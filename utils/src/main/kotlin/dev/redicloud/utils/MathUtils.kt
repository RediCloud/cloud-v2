package dev.redicloud.utils

import kotlin.random.Random

fun randomIntInRange(min: Int, max: Int): Int {
    return Random.nextInt(min, max + 1)
}