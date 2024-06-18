package dev.redicloud.updater.tasks.impl.v2_3_5

import dev.redicloud.console.Console
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.updater.BuildInfo
import dev.redicloud.updater.UpdateInfo
import dev.redicloud.updater.tasks.UpdateTask
import dev.redicloud.utils.CLOUD_VERSION

class AddClusterVersionUpdateTask : UpdateTask(
    BuildInfo("dev", -1, "2.3.4-SNAPSHOT"),
    BuildInfo("dev", -1, "2.3.5-SNAPSHOT")
) {
    override fun postUpdate(updateInfo: UpdateInfo, console: Console, databaseConnection: DatabaseConnection) {
        val buckets = getBucketsByClass("dev.redicloud.service.base.utils.ClusterConfiguration", databaseConnection)
        if (buckets.size != 1) {
            throw IllegalStateException("Expected 1 ClusterConfiguration bucket, but found ${buckets.size}")
        }
        val bucket = buckets.first()
        val gsonPackage = bucket.get()
        val jsonElement = gsonPackage.json

        jsonElement.asJsonObject.addProperty("cloud-version", CLOUD_VERSION)
        gsonPackage.json = jsonElement
        bucket.set(gsonPackage)
    }
}