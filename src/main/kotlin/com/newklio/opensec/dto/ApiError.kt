package com.newklio.opensec.dto

import java.time.Instant

data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null
)
