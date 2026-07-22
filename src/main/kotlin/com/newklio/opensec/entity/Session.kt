package com.newklio.opensec.entity

import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SessionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

/**
 * A behavioral session: the ordered sequence of events for one entity within a sliding activity
 * window (PRD §5). Partitioned logically by `(tenant, entity_type, entity_id)`.
 */
@Entity
@Table(
    name = "sessions",
    indexes = [
        Index(name = "idx_sessions_lookup", columnList = "tenant_id, entity_type, entity_id, status"),
    ],
)
data class Session(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    val entityType: EntityType,
    @Column(name = "entity_id", nullable = false)
    val entityId: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: SessionStatus = SessionStatus.OPEN,
    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,
    @Column(name = "last_event_at", nullable = false)
    var lastEventAt: Instant,
    @Column(name = "event_count", nullable = false)
    var eventCount: Int = 0,
    @Column(name = "auth_failure_count", nullable = false)
    var authFailureCount: Int = 0,
    @Column(nullable = false)
    var suspicious: Boolean = false,
    @Column(name = "risk_reason", length = 512)
    var riskReason: String? = null,
    @Column(name = "sequence_json", nullable = false, length = 1_000_000)
    var sequenceJson: String = "[]",
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
)
