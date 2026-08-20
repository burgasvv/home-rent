package org.burgas.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.IdentityPrincipal
import org.burgas.dto.IdentityRequest
import org.burgas.kafka.KafkaConfig
import org.burgas.kafka.KafkaTopics
import org.burgas.service.IdentityService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureIdentityRouter() {

    val identityService by inject<IdentityService>()
    val kafkaProducer = KafkaProducer<String, String>(KafkaConfig.getProducerProps())

    val identityAuthenticationInterceptPlugin = createRouteScopedPlugin("IdentityAuthenticationInterceptPlugin") {
        on(AuthenticationChecked) { call ->

            when(call.request.path()) {

                "/api/v1/identities/by-id", "/api/v1/identities/delete",
                "/api/v1/identities/upload-image", "/api/v1/identities/remove-image" -> {

                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept identity by identityId parameter"
                    }
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity[Uuid.parse(call.parameters["identityId"]!!)]
                    }
                    require(identityPrincipal.id == identity.id.value) {
                        "Not authorized intercept identity by identityId parameter"
                    }
                    return@on
                }

                "/api/v1/identities/update", "/api/v1/identities/change-password" -> {

                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept identity by IdentityRequest"
                    }
                    val identityRequest = call.receive<IdentityRequest>()
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity[identityRequest.uuid!!]
                    }
                    require(identityPrincipal.id == identity.id.value) {
                        "Not authorized intercept identity by IdentityRequest"
                    }
                    return@on
                }

                else -> return@on
            }
        }
    }

    routing {

        install(identityAuthenticationInterceptPlugin)

        route("/api/v1/identities") {

            authenticate("auth-session-admin", optional = true) {

                post("/create") {
                    val identityRequest = call.receive<IdentityRequest>()
                    val identityResponse = identityService.create(identityRequest)
                    val producerRecord = ProducerRecord(KafkaTopics.identityTopic.name(), "create-identity", Json.encodeToString(identityResponse))
                    kafkaProducer.send(producerRecord)
                    call.respond(HttpStatusCode.OK, identityResponse)
                }
            }

            authenticate("auth-session-admin") {

                get {
                    call.respond(HttpStatusCode.OK, identityService.findAll())
                }

                post("/create-admin") {
                    val identityRequest = call.receive<IdentityRequest>()
                    val identityResponse = identityService.createAdmin(identityRequest)
                    call.respond(HttpStatusCode.OK, identityResponse)
                }

                put("/change-status") {
                    val identityRequest = call.receive<IdentityRequest>()
                    identityService.changeStatus(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }

            authenticate("auth-session") {

                get("/by-id") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    val identityResponse = identityService.findById(identityId)
                    call.respond(HttpStatusCode.OK, identityResponse)
                }

                put("/update") {
                    val identityRequest = call.receive<IdentityRequest>()
                    val identityResponse = identityService.update(identityRequest)
                    call.respond(HttpStatusCode.OK, identityResponse)
                }

                post("/delete") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    identityService.delete(identityId)
                    call.respondRedirect("/api/v1/security/logout")
                }

                put("/change-password") {
                    val identityRequest = call.receive<IdentityRequest>()
                    identityService.changePassword(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }

                post("/upload-image") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    identityService.uploadImage(identityId, call.receiveMultipart(Long.MAX_VALUE))
                    call.respond(HttpStatusCode.OK)
                }

                delete("/remove-image") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    identityService.removeImage(identityId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}