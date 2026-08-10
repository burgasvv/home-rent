package org.burgas.serialization

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.uuid.Uuid

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
                serializersModule = SerializersModule { contextual(Uuid::class, UUIDSerializer) }
            }
        )
    }
}

object UUIDSerializer : KSerializer<Uuid> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parse(decoder.decodeString())
    }
}