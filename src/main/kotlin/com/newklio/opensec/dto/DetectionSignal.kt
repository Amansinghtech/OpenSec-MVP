package com.newklio.opensec.dto

import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SignalSeverity
import java.time.Instant
import java.util.UUID

const val DETECTION_SIGNAL_EVENT = "detection_signal"

/**
 * Output of the detection/policy engine (PRD §6): a scored security signal for an entity.
 */
data class DetectionSignal(
    val signalId: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val correlationId: String? = null,
    val detector: String,
    val signalType: String,
    val entityType: EntityType,
    val entityId: String,
    val riskScore: Int,
    val severity: SignalSeverity,
    val reason: String,
    val sourceRef: String? = null,
    val occurredAt: Instant,
)
