package org.burgas

import io.ktor.server.application.*
import org.burgas.database.configureDatabase
import org.burgas.kafka.configureKafkaConsumer
import org.burgas.koin.configureKoin
import org.burgas.router.*
import org.burgas.security.configureSecurity
import org.burgas.serialization.configureSerialization
import org.burgas.swagger.configureSwagger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.modules() {
    configureDatabase()
    configureSerialization()
    configureKafkaConsumer()
    configureSecurity()
    configureKoin()
    configureSwagger()
    configureSecurityRouter()
    configureImageRouter()
    configureVideoRouter()
    configureIdentityRouter()
    configureHomeRouter()
    configureMeetingRouter()
}
