package org.burgas

import io.ktor.server.application.Application
import org.burgas.serialization.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.modules() {
    configureSerialization()
}
