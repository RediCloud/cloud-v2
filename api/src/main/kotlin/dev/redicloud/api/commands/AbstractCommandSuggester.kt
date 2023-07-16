package dev.redicloud.api.commands

import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

abstract class AbstractCommandSuggester {

    private val cache: EasyCache<Array<String>, CommandContext> = EasyCache(5.seconds) {
        suggest(it!!)
    }

    fun preSuggest(context: CommandContext): Array<String> {
        return cache.get(context)!!
    }

    abstract fun suggest(context: CommandContext): Array<String>
}