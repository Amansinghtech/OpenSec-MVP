package com.newklio.opensec.dto

import com.newklio.opensec.model.SignalSeverity
import java.time.Instant
import java.util.UUID

const val CONTEXTUALIZED_DETECTION_EVENT = "contextualized_detection"

data class ContextualizedDetection(
    val id: UUID = UUID.randomUUID(),
    val signalId: UUID,
    val tenantId: UUID,
    val correlationId: String? = null,
    val detector: String,
    val riskScore: Int,
    val severity: SignalSeverity,
    val reason: String,
    val cveIds: List<String> = emptyList(),
    val contextSummary: String? = null,
    val occurredAt: Instant,
)
