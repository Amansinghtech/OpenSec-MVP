package com.newklio.opensec.service

import com.newklio.opensec.entity.AuthAuditLog
import com.newklio.opensec.entity.AuthEventType
import com.newklio.opensec.repository.AuthAuditLogRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthAuditService(
    private val authAuditLogRepository: AuthAuditLogRepository
) {

    fun record(
        eventType: AuthEventType,
        username: String?,
        tenantId: UUID? = null,
        request: HttpServletRequest? = null,
        detail: String? = null
    ) {
        authAuditLogRepository.save(
            AuthAuditLog(
                eventType = eventType,
                username = username,
                tenantId = tenantId,
                ipAddress = request?.let { clientIp(it) },
                userAgent = request?.getHeader("User-Agent")?.take(512),
                detail = detail?.take(512)
            )
        )
    }

    private fun clientIp(request: HttpServletRequest): String? {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) {
            forwarded.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
