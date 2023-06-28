package dev.redicloud.repository.server.version.utils

import dev.redicloud.utils.ConfigurationFileEditor
import java.io.File


data class CloudServerVersionType(
    val name: String,
    val craftBukkitBased: Boolean,
    val proxy: Boolean,
    val jvmArguments: List<String> = mutableListOf(),
    val programmArguments: List<String> = mutableListOf(),
    // key = file, pair first = key, pair second = value
    val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    val unknown: Boolean = false
) {
    companion object {
        val PAPER = CloudServerVersionType(
            "paper",
            true,
            false,
            mutableListOf("-Dcom.mojang.eula.agree=true", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableListOf("nogui", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableMapOf(
                "server.properties" to mutableMapOf(
                    "server-ip" to "%HOSTNAME%",
                    "server-port" to "%PORT%",
                    "online-mode" to "false"
                ),
                "bukkit.yml" to mutableMapOf("settings.connection-throttle" to "0"),
                "spigot.yml" to mutableMapOf("\"bungeecord\"" to "true")
            )
        )
        val SPIGOT = CloudServerVersionType(
            "spigot",
            true,
            false,
            mutableListOf("-Dcom.mojang.eula.agree=true", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableListOf("nogui", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableMapOf(
                "server.properties" to mutableMapOf(
                    "server-ip" to "%HOSTNAME%",
                    "server-port" to "%PORT%",
                    "online-mode" to "false"
                ),
                "bukkit.yml" to mutableMapOf("settings.connection-throttle" to "0"),
                "spigot.yml" to mutableMapOf("\"bungeecord\"" to "true")
            )
        )
        val BUKKIT = CloudServerVersionType(
            "bukkit",
            true,
            false,
            mutableListOf("-Dcom.mojang.eula.agree=true", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableListOf("nogui", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableMapOf(
                "server.properties" to mutableMapOf(
                    "server-ip" to "%HOSTNAME%",
                    "server-port" to "%PORT%",
                    "online-mode" to "false"
                ),
                "bukkit.yml" to mutableMapOf("settings.connection-throttle" to "0"),
                "spigot.yml" to mutableMapOf("\"bungeecord\"" to "true")
            )
        )
        val VELOCITY = CloudServerVersionType(
            "velocity",
            false,
            true,
            fileEdits = mutableMapOf(
                "velocity.toml" to mutableMapOf("bind" to "\"%HOSTNAME%:%PORT%\"")
            )
        )
        val BUNGEECORD = CloudServerVersionType(
            "bungeecord",
            false,
            true,
            fileEdits = mutableMapOf(
                "config.yml" to mutableMapOf(
                    "host" to "%HOSTNAME%",
                    "ip_forward" to "true"
                )
            )
        )
        val WATERFALL = CloudServerVersionType(
            "waterfall",
            false,
            true,
            fileEdits = mutableMapOf(
                "config.yml" to mutableMapOf(
                    "host" to "%HOSTNAME%",
                    "ip_forward" to "true"
                )
            )
        )
        val FOLIA = CloudServerVersionType(
            "folia",
            false,
            false,
            mutableListOf("-Dcom.mojang.eula.agree=true", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableListOf("nogui", "-Djline.terminal=jline.UnsupportedTerminal"),
            mutableMapOf(
                "server.properties" to mutableMapOf(
                    "server-ip" to "%HOSTNAME%",
                    "server-port" to "%PORT%"
                )
            )
        )
        val MINESTOM = CloudServerVersionType(
            "minestom",
            false,
            false
        )
        val LIMBO = CloudServerVersionType(
            "limbo",
            false,
            false
        )
        val UNKNOWN = CloudServerVersionType(
            "unknown",
            false,
            false,
            unknown = true
        )
        val VALUES = mutableListOf(
            PAPER,
            SPIGOT,
            BUKKIT,
            VELOCITY,
            BUNGEECORD,
            WATERFALL,
            FOLIA,
            MINESTOM,
            LIMBO,
            UNKNOWN
        )
    }

    fun doFileEdits(folder: File) {
        fileEdits.forEach { (file, editInfo) ->
            val fileToEdit = File(folder, file)
            if (!fileToEdit.exists()) return
            val editor = ConfigurationFileEditor.ofFile(fileToEdit) ?: return
            editInfo.forEach { key, value ->
                editor.setValue(key, value)
            }
            editor.saveToFile(fileToEdit)
        }
    }

}