package com.newklio.opensec.controller

import com.newklio.opensec.dto.AgentEnrollmentResponse
import com.newklio.opensec.dto.AgentResponse
import com.newklio.opensec.dto.CreateAgentRequest
import com.newklio.opensec.dto.FleetSummaryResponse
import com.newklio.opensec.dto.HeartbeatRequest
import com.newklio.opensec.entity.AgentPrincipal
import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.service.AgentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/agents")
class AgentController(
    private val agentService: AgentService,
) {
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun enroll(
        @Valid @RequestBody request: CreateAgentRequest,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): AgentEnrollmentResponse {
        val tenantId =
            user.tenantId
                ?: throw IllegalStateException("Admin user has no tenant")
        return agentService.enroll(request, tenantId)
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun listAgents(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): List<AgentResponse> {
        val tenantId =
            user.tenantId
                ?: throw IllegalStateException("User has no tenant")
        return agentService.listAgents(tenantId)
    }

    @GetMapping("/fleet/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun fleetSummary(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): FleetSummaryResponse {
        val tenantId =
            user.tenantId
                ?: throw IllegalStateException("User has no tenant")
        return agentService.fleetSummary(tenantId)
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('AGENT')")
    fun currentAgent(
        @AuthenticationPrincipal agent: AgentPrincipal,
    ): AgentResponse = agentService.getAgent(agent.agent.id!!, agent.tenantId)!!

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun getAgent(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): ResponseEntity<AgentResponse> {
        val tenantId =
            user.tenantId
                ?: throw IllegalStateException("User has no tenant")
        val agent = agentService.getAgent(id, tenantId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(agent)
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('AGENT')")
    fun heartbeat(
        @AuthenticationPrincipal agent: AgentPrincipal,
        @RequestBody request: HeartbeatRequest = HeartbeatRequest(),
    ): AgentResponse = agentService.heartbeat(agent.agent, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun revoke(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): ResponseEntity<Void> {
        val tenantId =
            user.tenantId
                ?: throw IllegalStateException("Admin user has no tenant")
        return if (agentService.revoke(id, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
