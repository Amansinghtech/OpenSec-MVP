package com.newklio.opensec.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

/**
 * A raw, source-agnostic log/event accepted by the ingestion API before normalization.
 * The original event is preserved verbatim as JSON in [payload] for replay/audit.
 */
@Entity
@Table(
    name = "raw_logs",
    indexes = [
        Index(name = "idx_raw_logs_tenant", columnList = "tenant_id"),
        Index(name = "idx_raw_logs_source", columnList = "source"),
    ],
)
data class RawLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false, length = 50)
    val source: String,
    @Column(name = "event_id")
    val eventId: String? = null,
    @Column
    val host: String? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "correlation_id")
    val correlationId: String? = null,
    @Column
    val severity: Int? = null,
    @Column
    val category: String? = null,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
    @Column(nullable = false, length = 1_000_000)
    val payload: String,
    @CreationTimestamp
    @Column(name = "received_at")
    val receivedAt: Instant? = null,
)
