package com.newklio.opensec.entity

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
import java.time.Instant
import java.util.UUID

enum class AuthEventType {
    SIGNUP,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    TOKEN_REFRESH,
    LOGOUT,
}

@Entity
@Table(
    name = "auth_audit_log",
    indexes = [
        Index(name = "idx_audit_username", columnList = "username"),
        Index(name = "idx_audit_event_type", columnList = "event_type"),
    ],
)
data class AuthAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: AuthEventType,
    @Column
    val username: String? = null,
    @Column(name = "tenant_id")
    val tenantId: UUID? = null,
    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,
    @Column(name = "user_agent", length = 512)
    val userAgent: String? = null,
    @Column(length = 512)
    val detail: String? = null,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
)
