package org.burgas.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.csrf.CSRF
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.burgas.database.Authority
import org.burgas.dto.ExceptionResponse
import org.burgas.dto.IdentityPrincipal

fun Application.configureSecurity() {

    val config = ApplicationConfig("application.yaml")

    authentication {
        session<IdentityPrincipal>("auth-session") {
            validate { it }
            challenge {
                val exceptionResponse = ExceptionResponse(
                    status = HttpStatusCode.Unauthorized.description,
                    code = HttpStatusCode.Unauthorized.value,
                    message = "Not authenticated by auth-session"
                )
                call.respond(HttpStatusCode.Unauthorized, exceptionResponse)
            }
        }
        session<IdentityPrincipal>("auth-session-admin") {
            validate { if (it.authority == Authority.ADMIN) it else null }
            challenge {
                val exceptionResponse = ExceptionResponse(
                    status = HttpStatusCode.Unauthorized.description,
                    code = HttpStatusCode.Unauthorized.value,
                    message = "Not authenticated by auth-session-admin"
                )
                call.respond(HttpStatusCode.Unauthorized, exceptionResponse)
            }
        }
    }

    install(Sessions) {
        cookie<IdentityPrincipal>("IDENTITY_PRINCIPAL") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = false
            cookie.extensions["SameSite"] = "Lax"
            transform(
                SessionTransportTransformerEncrypt(
                    config.property("cookie.identity.encryptionKey").getString().toByteArray(),
                    config.property("cookie.identity.signKey").getString().toByteArray()
                )
            )
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val exceptionResponse = ExceptionResponse(
                status = HttpStatusCode.BadRequest.description,
                code = HttpStatusCode.BadRequest.value,
                message = cause.message
            )
            call.respond(HttpStatusCode.BadRequest, exceptionResponse)
        }
    }

    install(DoubleReceive)

    install(CORS) {
        anyMethod()

        allowSameOrigin = true
        allowCredentials = true

        allowHeader(HttpHeaders.Host)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.ContentType)
    }

    install(CSRF) {
        allowOrigin("http://localhost:9000")
        this.onFailure { reason -> respond(HttpStatusCode.BadRequest, reason) }
    }
}