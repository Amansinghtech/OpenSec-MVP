package com.newklio.opensec.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "revoked_access_tokens")
data class RevokedAccessToken(
    @Id
    val jti: UUID,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
)
