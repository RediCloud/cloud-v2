package dev.redicloud.api.commands

import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

/**
 * A command suggester is used to suggest possible arguments for a command. This is used for tab completion.
 */
abstract class AbstractCommandSuggester {

    private val cache: EasyCache<Array<String>, CommandContext> = EasyCache(5.seconds) {
        suggest(it!!)
    }

    fun preSuggest(context: CommandContext): Array<String> {
        return cache.get(context)!!
    }

    /**
     * Suggests a list of possible arguments for the command. You don't need to check if the command is valid or if
     * its starts with the correct letter. This is already done by the command manager.
     * @param context The context of the command
     * @return The list of possible arguments
     */
    abstract fun suggest(context: CommandContext): Array<String>
}