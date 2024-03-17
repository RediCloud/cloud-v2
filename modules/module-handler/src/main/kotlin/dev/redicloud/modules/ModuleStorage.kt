package dev.redicloud.modules

import dev.redicloud.database.DatabaseConnection

class ModuleStorage(
    moduleData: ModuleData,
    databaseConnection: DatabaseConnection
) {

    private val map = databaseConnection.getClient().getLocalCachedMap(LocalCachedMapOpti)

}