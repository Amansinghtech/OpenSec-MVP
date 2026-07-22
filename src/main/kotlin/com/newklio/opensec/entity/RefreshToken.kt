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

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [Index(name = "idx_refresh_token_user", columnList = "user_id")],
)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false, unique = true)
    val token: String,
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(nullable = false)
    var revoked: Boolean = false,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
)
