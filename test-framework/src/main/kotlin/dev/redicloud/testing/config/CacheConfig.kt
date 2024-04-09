package dev.redicloud.testing.config

data class CacheConfig(
    val cacheServerVersionJars: Boolean = true,
    val cacheConnectorJars: Boolean = true,
    val cacheLibs: Boolean = true
)