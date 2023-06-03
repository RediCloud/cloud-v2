import dev.redicloud.commands.api.*
import java.util.UUID

fun main() {
    val commandManager = CommandManager()
    commandManager.register(TestCommand())

    val actor = ConsoleActor()

    /*
    commandManager.getCommands().forEach {
    println("Command: ${it.getName()}")
    println("Description: ${it.getDescription()}")
    println("Aliases: ${it.getAliases().joinToString(", ")}")
    it.getSubCommands().forEach { sub ->
        println("Sub command: ${sub.path}")
        println("\tDescription: ${sub.description}")
        println("\tAliases: ${sub.aliasePaths.joinToString(", ")}")
        sub.arguments.forEach { arg ->
            println("\tArgument: ${arg.name}")
            println("\t\tRequired: ${arg.required}")
            println("\t\tType: ${arg.clazz.qualifiedName}")
        }
    }
    println("Path without args:")
    it.getPathsWithArguments().forEach { path ->
        println("\t$path")
    }
        println("Paths with args:")
        it.getPaths().forEach { path ->
            println("\t$path")
        }
        println("")
    }
     */


    val response1 = commandManager.handleInput(actor, "test sub1 secondsub1 Ein")
    val response2 = commandManager.handleInput(actor, "test sub1 ss2 Elias")
    println(response1)
    println(response2)
}

class ConsoleActor() : ICommandActor<UUID> {
    override val identifier: UUID = UUID.randomUUID()

    override fun hasPermission(permission: String?): Boolean = true
}

@Command("test")
@CommandAlias(["test2", "test3"])
class TestCommand() : CommandBase() {

    @CommandSubPath("sub1 secondsub1")
    @CommandAlias(["sub1 ss1"])
    fun sub1(
        @CommandParameter(name = "message1", required = true) message1: String?,
        @CommandParameter(name = "message2", required = false) message2: String?,
        actor: ConsoleActor,
        @CommandParameter(name = "message3", required = false) message3: String?
    ) {
        println("S1: Actor: ${actor.identifier}")
        println("S1: Message1: $message1")
        println("S1: Message2: $message2")
        println("S1: Message3: $message3")
    }

    @CommandSubPath("sub1 secondsub2")
    @CommandAlias(["sub1 ss2"])
    fun sub2(
        @CommandParameter(name = "message1", required = true) message1: String?,
        @CommandParameter(name = "message2", required = false) message2: String?,
        @CommandParameter(name = "message3", required = false) message3: String?
    ) {
        println("S2: Message1: $message1")
        println("S2: Message2: $message2")
        println("S2: Message3: $message3")
    }

}