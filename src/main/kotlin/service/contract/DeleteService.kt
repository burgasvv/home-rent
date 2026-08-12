package org.burgas.service.contract

interface DeleteService<in ID> {

    suspend fun delete(id: ID)
}