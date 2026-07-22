package com.newklio.opensec.dto

import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.model.SessionStatus
import java.time.Instant
import java.util.UUID

const val SESSION_UPDATED_EVENT = "session_updated"
const val SUSPICIOUS_SESSION_EVENT = "suspicious_session_detected"

/** A single event's footprint within a session sequence. */
data class SessionEventRef(
    val eventType: NormalizedEventType,
    val occurredAt: Instant,
    val severity: Int? = null,
)

/** Serializable summary of a session, used as the payload for session events on the bus. */
data class SessionSummary(
    val sessionId: UUID,
    val tenantId: UUID,
    val entityType: EntityType,
    val entityId: String,
    val status: SessionStatus,
    val eventCount: Int,
    val authFailureCount: Int,
    val suspicious: Boolean,
    val riskReason: String? = null,
    val startedAt: Instant,
    val lastEventAt: Instant,
)
