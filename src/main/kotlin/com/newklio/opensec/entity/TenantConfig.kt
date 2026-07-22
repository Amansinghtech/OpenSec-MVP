package com.newklio.opensec.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tenant_configs")
data class TenantConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "config_key", nullable = false, length = 100)
    val configKey: String,
    @Column(name = "config_json", nullable = false, length = 10000)
    var configJson: String,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
)
