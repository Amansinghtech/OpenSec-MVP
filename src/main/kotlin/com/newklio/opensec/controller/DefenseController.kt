package com.newklio.opensec.controller

import com.newklio.opensec.dto.DefenseActionResult
import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.service.DefenseEngine
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/defense/actions")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
class DefenseController(
    private val defenseEngine: DefenseEngine,
) {
    @GetMapping
    fun listActions(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): List<DefenseActionResult> {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        return defenseEngine.listActions(tenantId)
    }
}
