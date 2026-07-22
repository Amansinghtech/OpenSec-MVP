package com.newklio.opensec.repository

import com.newklio.opensec.entity.AuthAuditLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuthAuditLogRepository : JpaRepository<AuthAuditLog, UUID>
