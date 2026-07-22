package com.newklio.opensec.entity

import com.newklio.opensec.model.AlertStatus
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
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "alerts",
    indexes = [
        Index(name = "idx_alerts_tenant", columnList = "tenant_id"),
        Index(name = "idx_alerts_status", columnList = "status"),
    ],
)
data class AlertRecord(
    @Id
    val id: UUID,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "correlation_id")
    val correlationId: String? = null,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, length = 2000)
    val description: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    val entityType: EntityType,
    @Column(name = "entity_id", nullable = false)
    val entityId: String,
    @Column(name = "risk_score", nullable = false)
    val riskScore: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val severity: SignalSeverity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AlertStatus = AlertStatus.NEW,
    @Column(name = "signal_ids", nullable = false, length = 4000)
    val signalIds: String,
    @Column(name = "cve_ids", length = 2000)
    val cveIds: String? = null,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
)
