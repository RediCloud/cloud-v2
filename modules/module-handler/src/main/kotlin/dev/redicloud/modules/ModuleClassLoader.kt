package dev.redicloud.modules

import dev.redicloud.libloader.boot.JarLoader
import java.net.URL
import java.net.URLClassLoader

class ModuleClassLoader(
    id: String,
    urls: Array<URL>,
    parent: ClassLoader
) : JarLoader, URLClassLoader(urls, parent){

    //TODO: set name of classloader

    override fun load(javaFile: URL?) = addURL(javaFile)

}