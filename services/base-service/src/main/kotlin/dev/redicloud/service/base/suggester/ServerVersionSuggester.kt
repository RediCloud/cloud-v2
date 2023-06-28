package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandSuggesterContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

class ServerVersionSuggester : ICommandSuggester {

    private val easyCache = EasyCache<Array<String>, Unit>(10.seconds) {
        ServerVersion.loadIfNotLoaded()
        ServerVersion.versions().map { it.name }.toTypedArray()
    }

    override fun suggest(context: CommandSuggesterContext): Array<String> = easyCache.get(Unit)!!


}