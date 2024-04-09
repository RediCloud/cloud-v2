group = "dev.redicloud.connector"

dependencies {
    shade(BuildDependencies.REDISSON) {
        exclude("com.fasterxml.jackson.core")
    }
    shade(BuildDependencies.GSON)
    shade(BuildDependencies.GUICE)
    shade(BuildDependencies.NETTY_HANDLER)
    shade(BuildDependencies.NETTY_RESOLVER_DNS)
    shade(BuildDependencies.NETTY_RESOLVER)
    shade(BuildDependencies.NETTY_TRANSPORT)
    shade(BuildDependencies.NETTY_BUFFER)
    shade(BuildDependencies.NETTY_CODEC)
    shade(BuildDependencies.NETTY_COMMON)
    shade(BuildDependencies.LOGBACK_CORE.withVersion("1.3.14"))
    shade(BuildDependencies.LOGBACK_CLASSIC.withVersion("1.3.14"))
}