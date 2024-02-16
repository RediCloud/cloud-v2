package dev.redicloud.modules

import com.google.gson.annotations.Expose
import java.io.File

data class ModuleDescription(
    val name: String,
    val id: String,
    val version: String,
    val description: String,
    val website: String? = null,
    val authors: List<String>,
    val mainClasses: HashMap<String, String>,
    @Expose(deserialize = false, serialize = false) var cachedFile: File? = null
)