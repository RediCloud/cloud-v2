package dev.redicloud.api.commands

interface ICommandArgument {

    val subCommand: ISubCommand
    val name: String
    val index: Int
    val required: Boolean
    val vararg: Boolean
    val annotatedSuggester: AbstractCommandSuggester
    val annotatedSuggesterParameter: Array<String>
    val actorArgument: Boolean
    val pathFormat: String

}