package dev.redicloud.modules.webinterface.service.user

import java.util.UUID

data class User(
    val id: UUID,
    var username: String,
    var password: String,
    var salt: String
) {

    fun checkPassword(password: String): Boolean {
        return this.password == password
    }

}