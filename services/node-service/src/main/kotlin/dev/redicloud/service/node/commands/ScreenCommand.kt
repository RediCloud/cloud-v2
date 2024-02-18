package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.Console
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.console.utils.Screen
import dev.redicloud.server.factory.screens.ServerScreen
import dev.redicloud.server.factory.screens.ServerScreenSuggester

@Command("screen")
@CommandAlias(["scr"])
@CommandDescription("Manage screen sessions")
class ScreenCommand(
    private val console: Console
) : ICommand {

    @CommandSubPath("list")
    @CommandDescription("List all screen sessions")
    fun list(
        actor: ConsoleActor
    ) {
        val screens = console.getScreens().filterIsInstance<ServerScreen>()
        if (screens.isEmpty()) {
            actor.sendMessage("§cThere are no server screen sessions running!")
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
        @CommandParameter("screen", true, ServerScreenSuggester::class) screen: Screen
    ) {
        if (screen.isActive()) {
            actor.sendMessage("§cThe screen session is already active!")
            return
        }
        if (!console.getCurrentScreen().isDefault()) {
            actor.sendMessage("§cYou are already in a screen session!")
            return
        }
        console.switchScreen(screen, true)
    }

    @CommandSubPath("leave")
    @CommandDescription("Leave the current screen session")
    fun leave(
        actor: ConsoleActor
    ) {
        val currentScreen = console.getCurrentScreen()
        if (currentScreen.isDefault()) {
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
        @CommandParameter("screen", true, ServerScreenSuggester::class) screen: Screen
    ) {
        if (screen.isDefault()) {
            actor.sendMessage("§cYou can't toggle the default screen session!")
            return
        }
        if (screen.isActive()) {
            console.switchToDefaultScreen(true)
            console.writeLine("You left the screen session ${toConsoleValue(screen.name)}")
            return
        }
        console.switchScreen(screen, true)
    }

    @CommandSubPath("execute <command...>")
    @CommandAlias(["exec <command...>", "write <command...>"])
    @CommandDescription("Execute a command in the current screen session")
    fun execute(
        actor: ConsoleActor,
        @CommandParameter("command", true) vararg command: String
    ) {
        val currentScreen = console.getCurrentScreen()
        if (currentScreen.isDefault() || currentScreen !is ServerScreen) {
            actor.sendMessage("§cYou are not in a server screen session!")
            return
        }
        if (command.isEmpty() || command.none { it.isNotBlank() }) {
            actor.sendMessage("§cYou need to specify a command!")
            return
        }
        currentScreen.executeCommand(command.joinToString(" "))
    }

}