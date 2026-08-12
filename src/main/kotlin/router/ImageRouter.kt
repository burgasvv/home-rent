package org.burgas.router

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.burgas.service.ImageService
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureImageRouter() {

    val imageService by inject<ImageService>()

    routing {

        route("/api/v1/images") {

            get("/by-id") {
                val imageId = Uuid.parse(call.parameters["imageId"]!!)
                val imageEntity = imageService.readEntity(imageId)
                call.respondBytes(ContentType.parse(imageEntity.contentType), HttpStatusCode.OK) {
                    imageEntity.data.bytes
                }
            }
        }
    }
}