package org.burgas.service

import org.burgas.dto.Response

interface FindService<in ID, out R : Response> {

    suspend fun findById(id: ID): R
}