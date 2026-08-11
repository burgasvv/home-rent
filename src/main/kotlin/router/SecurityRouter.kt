package org.burgas.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.*
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.database.IdentityTable
import org.burgas.dto.AuthRequest
import org.burgas.dto.IdentityPrincipal
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*

@OptIn(InternalAPI::class)
fun Application.configureSecurityRouter() {

    intercept(ApplicationCallPipeline.Setup) {
        when(call.request.httpMethod) {
            HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch, HttpMethod.Delete -> {
                call.request.setHeader(
                    "X-CSRF-Token", listOf(UUID.randomUUID().toString())
                )
            }
            else -> proceed()
        }
    }

    routing {

        route("/api/v1/security") {

            post("/login") {
                val identityPrincipal = call.sessions.get(IdentityPrincipal::class)
                if (identityPrincipal != null) {
                    call.respond(HttpStatusCode.OK, "You already logged in")
                } else {
                    val authRequest = call.receive<AuthRequest>()
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity.find { IdentityTable.email eq authRequest.email }.singleOrNull()
                    }
                    if (
                        identity != null && identity.status &&
                        BCrypt.checkpw(authRequest.password, identity.password)
                    ) {
                        val identityPrincipal = IdentityPrincipal(id = identity.id.value, authority = identity.authority)
                        call.sessions.set(identityPrincipal, IdentityPrincipal::class)
                        call.respond(
                            HttpStatusCode.OK,
                            "You successfully logged in: ${identityPrincipal.id}, ${identityPrincipal.authority}"
                        )
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Identity not found and not authenticated")
                    }
                }
            }

            authenticate("auth-session") {

                post("/logout") {
                    call.sessions.clear(IdentityPrincipal::class)
                    call.respond(HttpStatusCode.OK, "You successfully logged out")
                }
            }
        }
    }
}