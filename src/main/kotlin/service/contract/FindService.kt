package org.burgas.service.contract

import org.burgas.dto.Response

interface FindService<in ID, out R : Response> {

    suspend fun findById(id: ID): R
}