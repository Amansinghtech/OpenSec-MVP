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
@Table(name = "tenants")
data class Tenant(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false, unique = true)
    val slug: String,
    @Column(nullable = false)
    var enabled: Boolean = true,
    @CreationTimestamp
    val createdAt: Instant? = null,
    @UpdateTimestamp
    val updatedAt: Instant? = null,
)
