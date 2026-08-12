package org.burgas.service.contract

import io.ktor.http.content.PartData
import org.burgas.dao.File

interface UploadService<in I : PartData, out F : File> {

    suspend fun upload(fileItem: I): F
}