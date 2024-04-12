package dev.redicloud.api.modules


interface IModuleDescription {
    val name: String
    val id: String
    val version: String
    val description: String
    val website: String?
    val authors: List<String>
    val mainClasses: HashMap<String, String>
}