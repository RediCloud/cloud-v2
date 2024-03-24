group = "dev.redicloud.connector"

dependencies {
    shade(BuildDependencies.redisson) {
        exclude("com.fasterxml.jackson.core")
    }
    shade(BuildDependencies.gson)
    shade(BuildDependencies.guice)
    shade(BuildDependencies.nettyHandler)
    shade(BuildDependencies.nettyResolverDns)
    shade(BuildDependencies.nettyResolver)
    shade(BuildDependencies.nettyTransport)
    shade(BuildDependencies.nettyBuffer)
    shade(BuildDependencies.nettyCodec)
    shade(BuildDependencies.nettyCommon)
    shade(BuildDependencies.logbackCore.withVersion("1.3.14"))
    shade(BuildDependencies.logbackClassic.withVersion("1.3.14"))
}