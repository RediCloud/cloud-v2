package dev.redicloud.api.utils

import java.util.regex.Pattern

interface ProcessConfiguration {

    val jvmArguments: MutableList<String>
    val environmentVariables: MutableMap<String, String>
    val programParameters: MutableList<String>
    // URL -> Path
    val defaultFiles: MutableMap<String, String>
    // Path -> (edit key -> edit value)
    val fileEdits: MutableMap<String, MutableMap<String, String>>
    val references: List<ProcessConfiguration>? get() = null

    fun getLibPatterns(vararg files: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        if (files.isNotEmpty()) {
            patterns.add(Pattern.compile("(${files.joinToString("|")})"))
        }
        if (defaultFiles.keys.isNotEmpty()) {
            patterns.add(Pattern.compile("(${defaultFiles.keys.joinToString("|")})"))
        }
        references?.forEach {
            patterns.addAll(it.getLibPatterns())
            if (it.defaultFiles.isNotEmpty()) {
                patterns.add(Pattern.compile("(${it.defaultFiles.keys.joinToString("|")})"))
            }
        }
        return patterns
    }

    companion object {
        fun collect(vararg processConfigurations: ProcessConfiguration): ProcessConfiguration {
            return CollectedProcessConfiguration(processConfigurations.toList())
        }
    }

}

class CollectedProcessConfiguration(
    override val references: List<ProcessConfiguration>
) : ProcessConfiguration {
    override val jvmArguments: MutableList<String> = references.flatMap { it.jvmArguments }.toMutableList()
    override val environmentVariables: MutableMap<String, String> = references.flatMap { it.environmentVariables.toList() }.toMap().toMutableMap()
    override val programParameters: MutableList<String> = references.flatMap { it.programParameters }.toMutableList()
    override val defaultFiles: MutableMap<String, String> = references.flatMap { it.defaultFiles.toList() }.toMap().toMutableMap()
    override val fileEdits: MutableMap<String, MutableMap<String, String>> = references.flatMap { it.fileEdits.toList() }.toMap().toMutableMap()
}