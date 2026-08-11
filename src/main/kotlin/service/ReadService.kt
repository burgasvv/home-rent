package org.burgas.service

import org.jetbrains.exposed.v1.dao.UuidEntity

interface ReadService<in ID, out E : UuidEntity> {

    suspend fun readEntity(id: ID): E
}