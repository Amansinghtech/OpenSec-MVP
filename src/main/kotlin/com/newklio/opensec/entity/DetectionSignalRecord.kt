package com.newklio.opensec.entity

import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SignalSeverity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "detection_signals",
    indexes = [
        Index(name = "idx_signals_tenant_entity", columnList = "tenant_id, entity_type, entity_id"),
        Index(name = "idx_signals_severity", columnList = "severity"),
    ],
)
data class DetectionSignalRecord(
    @Id
    val id: UUID,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "correlation_id")
    val correlationId: String? = null,
    @Column(nullable = false, length = 40)
    val detector: String,
    @Column(name = "signal_type", nullable = false, length = 60)
    val signalType: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    val entityType: EntityType,
    @Column(name = "entity_id", nullable = false)
    val entityId: String,
    @Column(name = "risk_score", nullable = false)
    val riskScore: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val severity: SignalSeverity,
    @Column(nullable = false, length = 512)
    val reason: String,
    @Column(name = "source_ref", length = 255)
    val sourceRef: String? = null,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
)
