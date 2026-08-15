package org.burgas.router

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.burgas.dao.HomeEntity
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.EntityFileRequest
import org.burgas.dto.HomeRequest
import org.burgas.dto.IdentityPrincipal
import org.burgas.service.HomeService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureHomeRouter() {

    val homeService by inject<HomeService>()

    val homeAuthenticationInterceptPlugin = createRouteScopedPlugin("HomeAuthenticationInterceptPlugin") {
        on(AuthenticationChecked) { call ->
            when (call.request.path()) {

                "/api/v1/homes/by-id" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept home by id"
                    }
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val homeEntity = HomeEntity[Uuid.parse(call.parameters["homeId"]!!)]
                        if (homeEntity.tenant != null) {
                            require(
                                identityPrincipal.id == homeEntity.lessor.id.value ||
                                        identityPrincipal.id == homeEntity.tenant!!.id.value
                            ) {
                                "Not authorized intercept home by lessor id"
                            }
                        } else {
                            return@suspendTransaction
                        }
                    }
                }

                "/api/v1/homes/delete", "/api/v1/homes/upload-preview-image", "/api/v1/homes/remove-preview-image",
                "/api/v1/homes/cancel", "/api/v1/homes/upload-images", "/api/v1/homes/upload-videos" -> {

                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept home by homeId parameter"
                    }
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val homeEntity = HomeEntity[Uuid.parse(call.parameters["homeId"]!!)]

                        require(identityPrincipal.id == homeEntity.lessor.id.value) {
                            "Not authorized intercept home by homeId parameter"
                        }
                    }
                }

                "/api/v1/homes/create" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept home create by HomeRequest body"
                    }
                    val homeRequest = call.receive<HomeRequest>()
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val identityEntity = IdentityEntity[homeRequest.lessorUuid!!]

                        require(identityPrincipal.id == identityEntity.id.value) {
                            "Not authorized intercept home create by HomeRequest body"
                        }
                    }
                }

                "/api/v1/homes/update", "/api/v1/homes/rent" -> {
                    val identityPrincipal = requireNotNull(call.principal<IdentityPrincipal>()) {
                        "Not authenticated intercept home by HomeRequest body"
                    }
                    val homeRequest = call.receive<HomeRequest>()
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val homeEntity = HomeEntity[homeRequest.uuid!!]

                        require(identityPrincipal.id == homeEntity.lessor.id.value) {
                            "Not authorized intercept home by HomeRequest body"
                        }
                    }
                }

                else -> return@on
            }
        }
    }

    routing {

        install(homeAuthenticationInterceptPlugin)

        route("/api/v1/homes") {

            get("/by-free") {
                call.respond(HttpStatusCode.OK, homeService.findAllFree())
            }

            authenticate("auth-session-admin") {

                get {
                    call.respond(HttpStatusCode.OK, homeService.findAll())
                }
            }

            authenticate("auth-session") {

                get("/by-id") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    call.respond(HttpStatusCode.OK, homeService.findById(homeId))
                }

                post("/create") {
                    val homeRequest = call.receive<HomeRequest>()
                    val homeResponse = homeService.create(homeRequest)
                    call.respond(HttpStatusCode.OK, homeResponse)
                }

                put("/update") {
                    val homeRequest = call.receive<HomeRequest>()
                    val homeResponse = homeService.update(homeRequest)
                    call.respond(HttpStatusCode.OK, homeResponse)
                }

                delete("/delete") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    homeService.delete(homeId)
                    call.respond(HttpStatusCode.OK)
                }

                put("/rent") {
                    val homeRequest = call.receive<HomeRequest>()
                    homeService.rent(homeRequest)
                    call.respond(HttpStatusCode.OK)
                }

                put("/cancel") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    homeService.cancel(homeId)
                    call.respond(HttpStatusCode.OK)
                }

                post("/upload-preview-image") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    homeService.uploadPreviewImage(homeId, call.receiveMultipart(Long.MAX_VALUE))
                    call.respond(HttpStatusCode.OK)
                }

                delete("/remove-preview-image") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    homeService.removePreviewImage(homeId)
                    call.respond(HttpStatusCode.OK)
                }

                post("/upload-images") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    homeService.uploadImages(homeId, call.receiveMultipart(Long.MAX_VALUE))
                    call.respond(HttpStatusCode.OK)
                }

                delete("/remove-images") {
                    val entityFileRequest = call.receive<EntityFileRequest>()
                    homeService.removeImages(entityFileRequest)
                    call.respond(HttpStatusCode.OK)
                }

                post("/upload-videos") {
                    val homeId = Uuid.parse(call.parameters["homeId"]!!)
                    homeService.uploadVideos(homeId, call.receiveMultipart(Long.MAX_VALUE))
                    call.respond(HttpStatusCode.OK)
                }

                delete("/remove-videos") {
                    val entityFileRequest = call.receive<EntityFileRequest>()
                    homeService.removeVideos(entityFileRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}