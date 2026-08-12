package org.burgas.koin

import io.ktor.server.application.*
import org.burgas.service.IdentityService
import org.burgas.service.ImageService
import org.burgas.service.VideoService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureKoin() {

    val module = module {
        singleOf(::ImageService)
        singleOf(::VideoService)
        single { IdentityService(imageService = get(ImageService::class)) }
    }

    install(Koin) {
        modules(modules = module)
    }
}