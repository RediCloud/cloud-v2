package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.Console
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.server.factory.screens.ServerScreen
import dev.redicloud.server.factory.screens.ServerScreenSuggester

@Command("screen")
@CommandAlias(["scr"])
@CommandDescription("Manage screen sessions")
class ScreenCommand(
    private val console: Console
) : CommandBase() {

    @CommandSubPath("list")
    @CommandDescription("List all screen sessions")
    fun list(
        actor: ConsoleActor
    ) {
        val screens = console.getScreens().filterIsInstance<ServerScreen>()
        if (screens.isEmpty()) {
            actor.sendMessage("§cThere are no screen sessions running!")
            return
        }
        actor.sendHeader("Screen Sessions")
        actor.sendMessage("")
        screens.forEach { screen ->
            actor.sendMessage("§8- %hc%${screen.name} §8(%tc%${screen.serviceId.toName()}§8)")
        }
        actor.sendMessage("")
        actor.sendHeader("Screen Sessions")
    }

    @CommandSubPath("join <screen>")
    @CommandDescription("Join a screen session")
    fun join(
        actor: ConsoleActor,
        @CommandParameter("screen", true, ServerScreenSuggester::class) serverScreen: ServerScreen
    ) {
        if (serverScreen.isActive()) {
            actor.sendMessage("§cThe screen session is already active!")
            return
        }
        if (!console.getCurrentScreen().isDefault()) {
            actor.sendMessage("§cYou are already in this screen session!")
            return
        }
        console.switchScreen(serverScreen, true)
    }

    @CommandSubPath("leave")
    @CommandDescription("Leave the current screen session")
    fun leave(
        actor: ConsoleActor
    ) {
        val currentScreen = console.getCurrentScreen()
        if (currentScreen !is ServerScreen) {
            actor.sendMessage("§cYou are not in a screen session!")
            return
        }
        console.switchToDefaultScreen(true)
        console.writeLine("You left the screen session ${toConsoleValue(currentScreen.name)}")
    }

    @CommandSubPath("toggle <screen>")
    @CommandDescription("Toggle a screen session")
    fun toggle(
        actor: ConsoleActor,
        @CommandParameter("screen", true, ServerScreenSuggester::class) serverScreen: ServerScreen
    ) {
        if (serverScreen.isDefault()) {
            actor.sendMessage("§cYou can't toggle the default screen session!")
            return
        }
        if (serverScreen.isActive()) {
            console.switchToDefaultScreen(true)
            console.writeLine("You left the screen session ${toConsoleValue(serverScreen.name)}")
            return
        }
        console.switchScreen(serverScreen, true)
    }

}