package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.repository.server.version.utils.ServerVersion
import kotlinx.coroutines.runBlocking

class ServerVersionSuggester : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        runBlocking { ServerVersion.loadIfNotLoaded() }
        return ServerVersion.versions().map { it.name }.toTypedArray()
    }


}