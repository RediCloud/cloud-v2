package dev.redicloud.server.factory.utils

import dev.redicloud.api.utils.factory.StartResultType
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.server.factory.ServerProcess
import java.util.UUID

open class StartResult(val type: StartResultType)
class SuccessStartResult(val server: CloudServer, val process: ServerProcess) : StartResult(StartResultType.SUCCESS)
class AlreadyRunningStartResult(val server: CloudServer) : StartResult(StartResultType.ALREADY_RUNNING)
class NotEnoughRamOnNodeStartResult : StartResult(StartResultType.RAM_USAGE_TOO_HIGH)
class NotEnoughRamOnJVMStartResult : StartResult(StartResultType.RAM_USAGE_TOO_HIGH)
class TooMuchServicesOfTemplateStartResult : StartResult(StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
class TooMuchServicesOfTemplateOnNodeStartResult: StartResult(StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
class NodeIsNotAllowedStartResult : StartResult(StartResultType.NODE_IS_NOT_ALLOWED)
class UnknownServerVersionStartResult(val serverVersionId: UUID?) : StartResult(StartResultType.UNKNOWN_SERVER_VERSION)
class UnknownJavaVersionStartResult(val javaVersionId: UUID?) : StartResult(StartResultType.UNKNOWN_JAVA_VERSION)
class JavaVersionNotInstalledStartResult(val javaVersion: CloudJavaVersion) : StartResult(StartResultType.JAVA_VERSION_NOT_INSTALLED)
class UnknownServerVersionTypeStartResult(val serverVersionTypeId: UUID?) : StartResult(StartResultType.UNKNOWN_SERVER_TYPE_VERSION)
class UnknownErrorStartResult(val throwable: Throwable) : StartResult(StartResultType.UNKNOWN_ERROR)

class UnknownServerVersionHandlerResult(val serverVersionTypeId: UUID?) : StartResult(StartResultType.UNKNOWN_SERVER_VERSION_HANDLER)

class StoppedStartResult : StartResult(StartResultType.STOPPED)

class UnknownConfigurationTemplateStartResult(val uniqueId: UUID) : StartResult(StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE)