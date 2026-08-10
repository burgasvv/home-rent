package org.burgas

import io.ktor.server.application.Application
import org.burgas.database.configureDatabase
import org.burgas.router.configureSecurityRouter
import org.burgas.security.configureSecurity
import org.burgas.serialization.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.modules() {
    configureDatabase()
    configureSerialization()
    configureSecurity()
    configureSecurityRouter()
}
