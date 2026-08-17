package org.burgas.router

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.burgas.dao.HomeEntity
import org.burgas.dao.MeetingEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.IdentityPrincipal
import org.burgas.dto.MeetingRequest
import org.burgas.service.MeetingService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureMeetingRouter() {

    val meetingService by inject<MeetingService>()

    val meetingAuthenticationInterceptPlugin = createRouteScopedPlugin("MeetingAuthenticationInterceptPlugin") {
        on(AuthenticationChecked) { call ->
            when (call.request.path()) {

                "/api/v1/meetings/by-id" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept meeting by id"
                    }
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val meetingEntity = MeetingEntity[Uuid.parse(call.parameters["meetingId"]!!)]
                        require(
                            identityPrincipal.id == meetingEntity.home.lessor.id.value ||
                                    identityPrincipal.id == meetingEntity.applicant.id.value
                        ) {
                            "Not authorized intercept meeting by id"
                        }
                    }
                }

                "/api/v1/meetings/delete" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept meeting delete by id"
                    }
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val meetingEntity = MeetingEntity[Uuid.parse(call.parameters["meetingId"]!!)]
                        require(identityPrincipal.id == meetingEntity.home.lessor.id.value) {
                            "Not authorized intercept meeting delete by id"
                        }
                    }
                }

                "/api/v1/meetings/create" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept meeting create by MeetingRequest body"
                    }
                    val meetingRequest = call.receive<MeetingRequest>()
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val homeEntity = HomeEntity[meetingRequest.homeUuid!!]

                        require(identityPrincipal.id == homeEntity.lessor.id.value) {
                            "Not authorized intercept meeting create by MeetingRequest body"
                        }
                    }
                }

                "/api/v1/meetings/update" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept meeting update by MeetingRequest body"
                    }
                    val meetingRequest = call.receive<MeetingRequest>()
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val meetingEntity = MeetingEntity[meetingRequest.uuid!!]

                        require(identityPrincipal.id == meetingEntity.home.lessor.id.value) {
                            "Not authorized intercept meeting update by MeetingRequest body"
                        }
                    }
                }

                else -> return@on
            }
        }
    }

    routing {

        install(meetingAuthenticationInterceptPlugin)

        route("/api/v1/meetings") {

            authenticate("auth-session") {

                get("/by-id") {
                    val meetingId = Uuid.parse(call.parameters["meetingId"]!!)
                    call.respond(HttpStatusCode.OK, meetingService.findById(meetingId))
                }

                post("/create") {
                    val meetingRequest = call.receive<MeetingRequest>()
                    val meetingResponse = meetingService.create(meetingRequest)
                    call.respond(HttpStatusCode.OK, meetingResponse)
                }

                put("/update") {
                    val meetingRequest = call.receive<MeetingRequest>()
                    val meetingResponse = meetingService.update(meetingRequest)
                    call.respond(HttpStatusCode.OK, meetingResponse)
                }

                delete("/delete") {
                    val meetingId = Uuid.parse(call.parameters["meetingId"]!!)
                    meetingService.delete(meetingId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}