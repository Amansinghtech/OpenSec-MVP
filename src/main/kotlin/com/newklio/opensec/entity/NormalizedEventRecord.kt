package com.newklio.opensec.entity

import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
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

/**
 * Persisted normalized event (for replay/audit/search; the event bus remains the primary transport).
 */
@Entity
@Table(
    name = "normalized_events",
    indexes = [
        Index(name = "idx_norm_tenant_entity", columnList = "tenant_id, entity_type, entity_id"),
        Index(name = "idx_norm_event_type", columnList = "event_type"),
    ],
)
data class NormalizedEventRecord(
    @Id
    val id: UUID,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "correlation_id")
    val correlationId: String? = null,
    @Column(nullable = false, length = 50)
    val source: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    val eventType: NormalizedEventType,
    @Column
    val category: String? = null,
    @Column
    val severity: Int? = null,
    @Column
    val host: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    val entityType: EntityType,
    @Column(name = "entity_id", nullable = false)
    val entityId: String,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
    @Column(name = "attributes", nullable = false, length = 1_000_000)
    val attributes: String,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
)
