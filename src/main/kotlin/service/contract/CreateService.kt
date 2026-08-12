package org.burgas.service.contract

import org.burgas.dto.Request
import org.burgas.dto.Response

interface CreateService<in Req: Request, out Res : Response> {

    suspend fun create(request: Req): Res
}