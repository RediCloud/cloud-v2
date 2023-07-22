package dev.redicloud.utils

inline fun <reified T, B> T.mapTo(block: (T) -> B?): B? = block(this)