package org.burgas.security

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.csrf.CSRF
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.burgas.dto.ExceptionResponse

fun Application.configureSecurity() {

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
        allowHeader("X-CSRF-Token")

        allowXHttpMethodOverride()
        allowHost("localhost:9000", listOf("http", "https"))
    }

    install(CSRF) {
        allowOrigin("http://localhost:9000")
        checkHeader("X-CSRF-Token")
        onFailure { reason -> respond(HttpStatusCode.BadRequest, reason) }
    }
}