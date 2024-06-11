package dev.redicloud.modules

import com.google.gson.annotations.Expose
import dev.redicloud.api.modules.IModuleDescription
import java.io.File

data class ModuleDescription(
    override val name: String,
    override val id: String,
    override val version: String,
    override val description: String,
    override val website: String? = null,
    override val authors: List<String>,
    override val mainClasses: HashMap<String, String>,
    @Expose(deserialize = false, serialize = false) var cachedFile: File? = null
) : IModuleDescription