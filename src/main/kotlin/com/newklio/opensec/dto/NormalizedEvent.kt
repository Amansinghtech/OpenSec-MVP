package com.newklio.opensec.dto

import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import java.time.Instant
import java.util.UUID

/**
 * A raw log converted into a structured security event. Published to the `normalized_events` topic
 * and consumed by session reconstruction (Phase 8) and detection (Phase 9).
 */
data class NormalizedEvent(
    val eventId: UUID,
    val tenantId: UUID,
    val correlationId: String? = null,
    val source: String,
    val eventType: NormalizedEventType,
    val category: String? = null,
    val severity: Int? = null,
    val host: String? = null,
    val entityType: EntityType,
    val entityId: String,
    val occurredAt: Instant,
    val attributes: Map<String, Any?> = emptyMap(),
)
