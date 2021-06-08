package org.veriblock.miners.pop.api.auth

import com.papsign.ktor.openapigen.model.Described
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic

fun Application.installAuth(
    basicAuthProvider: BasicProvider,
    authUsername: String?,
    authPassword: String?
) {
    install(Authentication) {
        basic(basicAuthProvider.authName) {
            validate { credentials ->
                if (authUsername != null && authPassword != null &&
                    credentials.name == authUsername && credentials.password == authPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
}

enum class Scopes(override val description: String) : Described
