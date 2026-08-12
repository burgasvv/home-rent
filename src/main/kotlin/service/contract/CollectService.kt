package org.burgas.service.contract

import org.burgas.dto.Response

interface CollectService<out R : Response> {

    suspend fun findAll(): List<R>
}