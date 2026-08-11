package org.burgas.service

interface DeleteService<in ID> {

    suspend fun delete(id: ID)
}