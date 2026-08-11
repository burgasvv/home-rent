package org.burgas.serialization

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                useArrayPolymorphism = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
                allowTrailingComma = true
                prettyPrint = true
                allowSpecialFloatingPointValues = true
                allowComments = true
            }
        )
    }
}