package com.newklio.opensec.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

/**
 * Source-agnostic ingestion contract. `payload` carries the original event verbatim; the other
 * fields are extracted metadata used for routing/dedupe. `tenantId`/`correlationId` are derived
 * server-side from the authenticated request context, not the client.
 */
data class IngestLogRequest(
    @field:NotBlank(message = "source is required")
    val source: String,
    val host: String? = null,
    val eventId: String? = null,
    val occurredAt: Instant? = null,
    val severity: Int? = null,
    val category: String? = null,
    val payload: Map<String, Any?> = emptyMap(),
)

data class IngestResult(
    val id: UUID?,
    val duplicate: Boolean,
)

data class BatchIngestResult(
    val accepted: Int,
    val duplicates: Int,
    val ids: List<UUID>,
)
