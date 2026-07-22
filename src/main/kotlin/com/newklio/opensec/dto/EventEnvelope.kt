package com.newklio.opensec.dto

import java.time.Instant
import java.util.UUID

/**
 * Versioned envelope wrapping every event published to the bus. Carries the multi-tenant routing
 * metadata (`tenantId`, `correlationId`) that the PRD requires on every event.
 */
data class EventEnvelope(
    val eventType: String,
    val tenantId: UUID,
    val correlationId: String? = null,
    val source: String? = null,
    val occurredAt: Instant,
    val payload: Map<String, Any?> = emptyMap(),
    val schemaVersion: Int = 1,
    val eventId: UUID = UUID.randomUUID(),
    val emittedAt: Instant = Instant.now(),
)
