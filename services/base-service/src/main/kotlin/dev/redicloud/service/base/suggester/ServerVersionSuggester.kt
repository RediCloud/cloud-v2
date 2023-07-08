package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.server.version.utils.ServerVersion
import kotlinx.coroutines.runBlocking

class ServerVersionSuggester : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        runBlocking { ServerVersion.loadIfNotLoaded() }
        return ServerVersion.versions().map { it.name }.toTypedArray()
    }


}