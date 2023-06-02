import dev.redicloud.commands.api.*

fun main() {
    val commandManager = CommandManager()
    commandManager.register(TestCommand())
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
        /*
        println("Path without args:")
        it.getPathsWithArguments().forEach { path ->
            println("\t$path")
        }
         */
        println("Paths with args:")
        it.getPaths().forEach { path ->
            println("\t$path")
        }
        println("")
    }
}


@Command("test")
@CommandAlias(["test2", "test3"])
class TestCommand() : CommandBase() {

    @CommandSubPath("sub1 secondsub1")
    @CommandAlias(["sub1 secondsub2", "sub1 secondsub3"])
    fun sub1(
        @CommandParameter(name = "message1", required = true) message1: String?,
        @CommandParameter(name = "message2", required = false) message2: String?,
        @CommandParameter(name = "message3", required = false) message3: String?
    ) {
        println("Sub1")
    }

}