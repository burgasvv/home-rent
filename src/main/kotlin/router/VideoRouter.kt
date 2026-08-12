package org.burgas.router

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.burgas.service.VideoService
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureVideoRouter() {

    val videoService by inject<VideoService>()

    routing {

        route("/api/v1/videos") {

            get("/by-id") {
                val videoId = Uuid.parse(call.parameters["videoId"]!!)
                val videoEntity = videoService.readEntity(videoId)
                call.respondBytes(ContentType.parse(videoEntity.contentType), HttpStatusCode.OK) {
                    videoEntity.data.bytes
                }
            }
        }
    }
}