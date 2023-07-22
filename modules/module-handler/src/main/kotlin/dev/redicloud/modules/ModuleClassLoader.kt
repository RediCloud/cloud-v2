package dev.redicloud.modules

import dev.redicloud.libloader.boot.JarLoader
import java.io.File
import java.net.URL
import java.net.URLClassLoader

class ModuleClassLoader(
    val data: ModuleData,
    file: File,
    parent: ClassLoader
) : JarLoader, URLClassLoader(arrayOf(file.toURI().toURL()), parent){

    lateinit var tasks: List<ModuleTaskData>

    fun init(tasks: List<ModuleTaskData>) {
        this.tasks = tasks
    }

    override fun load(javaFile: URL?) = addURL(javaFile)

}