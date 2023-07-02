import dev.redicloud.commands.api.*
import java.util.UUID

fun main() {

    val actor = ConsoleActor()
    val commandManager = object : CommandManager<ConsoleActor>() {
        override fun getActor(identifier: ConsoleActor): ConsoleActor = actor
    }
    commandManager.register(TestCommand())

    while (true) {
        val input = readLine() ?: continue
        val response = commandManager.handleInput(actor, input)
        println(response)
    }
}

class ConsoleActor() : ICommandActor<UUID> {
    override val identifier: UUID = UUID.randomUUID()

    override fun hasPermission(permission: String?): Boolean = true

    override fun sendMessage(text: String) = println(text)

    override fun sendHeader(text: String) = println("Header: $text")
}

@Command("test")
@CommandAlias(["test2", "test3"])
class TestCommand() : CommandBase() {

    @CommandSubPath("sub1 secondsub1")
    @CommandAlias(["sub1 ss1"])
    fun sub1(
        actor: ConsoleActor,
        @CommandParameter(name = "message1", required = true) message1: String?,
        @CommandParameter(name = "message2", required = true) message2: String?,
        @CommandParameter(name = "message3", required = true) message3: String?,
        @CommandParameter(name = "intrange", required = true, suggester = IntegerSuggester::class,
            suggesterArguments = ["1", "100", "20"]) // min: 1, max: 100, step: 20
        range: Int
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