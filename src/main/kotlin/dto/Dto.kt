package org.burgas.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExceptionResponse(
    val status: String,
    val code: Int,
    val message: String?
)