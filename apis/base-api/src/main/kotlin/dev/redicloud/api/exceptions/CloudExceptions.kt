package dev.redicloud.api.exceptions

/** Base exception for all RediCloud operations. */
open class CloudException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when a database connection or query fails. */
class CloudDatabaseException(message: String, cause: Throwable? = null) : CloudException(message, cause)

/** Thrown when a server lifecycle operation (start/stop/delete/transfer) fails. */
class CloudServerException(message: String, cause: Throwable? = null) : CloudException(message, cause)

/** Thrown when a version download or patch operation fails. */
class CloudVersionException(message: String, cause: Throwable? = null) : CloudException(message, cause)

/** Thrown when a module lifecycle operation (load/unload/reload) fails. */
class CloudModuleException(message: String, cause: Throwable? = null) : CloudException(message, cause)
