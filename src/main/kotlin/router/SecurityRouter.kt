package org.burgas.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.utils.io.InternalAPI
import java.util.UUID

@OptIn(InternalAPI::class)
fun Application.configureSecurityRouter() {

    intercept(ApplicationCallPipeline.Setup) {

        when(call.request.httpMethod) {
            HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch, HttpMethod.Delete -> {
                call.request.setHeader(
                    "X-CSRF-Token", listOf(UUID.randomUUID().toString())
                )
                proceed()
            }
            else -> proceed()
        }
    }
}