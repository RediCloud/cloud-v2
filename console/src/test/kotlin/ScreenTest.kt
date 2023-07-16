import dev.redicloud.commands.api.*
import dev.redicloud.console.Console
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.Screen

fun main() {
    val console = Console("testNode", null)
    CommandArgumentParser.PARSERS[Screen::class] = ScreenArgumentParser(console)
    console.commandManager.register(ScreenCommand(console))
}

class ScreenArgumentParser(val console: Console) : ICommandArgumentParser<Screen> {
    override fun parse(parameter: String): Screen? = console.getScreens().firstOrNull { it.name == parameter.lowercase() }
}

@Command("screen")
@CommandAlias(["scr"])
class ScreenCommand(val console: Console) : CommandBase() {

    @CommandSubPath("join")
    fun join(
        actor: ConsoleActor,
        @CommandParameter("screen", true) screen: Screen
    ) {
        if (screen.isActive()) {
            actor.sendMessage("Screen is already active!")
            return
        }
        if (screen.isDefault()) {
            actor.sendMessage("Can´t join default screen! Use 'screen leave' to leave the screen")
            return
        }
        actor.sendMessage("Joined screen ${screen.name}")
        screen.display()
    }

    @CommandSubPath("list")
    fun list(
        actor: ConsoleActor
    ) {
        actor.sendMessage("Screens:")
        console.getScreens().forEach {
            actor.sendMessage(" - ${it.name} | ${if (it.isActive()) "active" else "inactive"}")
        }
    }

    @CommandSubPath("leave")
    fun leave(
        actor: ConsoleActor
    ) {
        if (console.getCurrentScreen().isDefault()) {
            actor.sendMessage("Can´t leave default screen!")
            return
        }
        console.getScreens().first { it.isDefault() }.display()
        actor.sendMessage("Left screen ${console.getCurrentScreen().name}")
    }


}