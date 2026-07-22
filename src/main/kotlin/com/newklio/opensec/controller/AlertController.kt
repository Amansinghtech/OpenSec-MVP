package com.newklio.opensec.controller

import com.newklio.opensec.dto.Alert
import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.model.AlertStatus
import com.newklio.opensec.service.AlertService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UpdateAlertStatusRequest(
    val status: AlertStatus,
)

@RestController
@RequestMapping("/alerts")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
class AlertController(
    private val alertService: AlertService,
) {
    @GetMapping
    fun listAlerts(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): List<Alert> {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        return alertService.listAlerts(tenantId)
    }

    @GetMapping("/{id}")
    fun getAlert(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): ResponseEntity<Alert> {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        val alert = alertService.getAlert(id, tenantId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(alert)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody request: UpdateAlertStatusRequest,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): ResponseEntity<Alert> {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        val alert = alertService.updateStatus(id, tenantId, request.status) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(alert)
    }
}
