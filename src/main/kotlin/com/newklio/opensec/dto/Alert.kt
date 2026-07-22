package com.newklio.opensec.dto

import com.newklio.opensec.model.AlertStatus
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SignalSeverity
import java.time.Instant
import java.util.UUID

const val ALERT_CREATED_EVENT = "alert_created"

data class Alert(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val correlationId: String? = null,
    val title: String,
    val description: String,
    val entityType: EntityType,
    val entityId: String,
    val riskScore: Int,
    val severity: SignalSeverity,
    val status: AlertStatus = AlertStatus.NEW,
    val signalIds: List<UUID> = emptyList(),
    val cveIds: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
