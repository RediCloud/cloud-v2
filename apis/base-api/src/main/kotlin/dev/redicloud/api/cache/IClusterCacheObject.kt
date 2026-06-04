package dev.redicloud.api.cache

import java.io.Serializable

/**
 * Marker interface for objects that can be stored in a cluster-wide cache.
 *
 * Entity classes in the repository layer implement this interface to indicate
 * they support cross-node caching with TTL-based invalidation.
 */
interface IClusterCacheObject : Serializable
