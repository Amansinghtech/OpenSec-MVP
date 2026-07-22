package com.newklio.opensec.controller

import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.entity.TenantConfig
import com.newklio.opensec.service.ConfigService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class UpsertConfigRequest(
    val configJson: String,
)

@RestController
@RequestMapping("/admin/config")
@PreAuthorize("hasRole('ADMIN')")
class ConfigController(
    private val configService: ConfigService,
) {
    @GetMapping
    fun listConfigs(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): Map<String, String> {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        return configService.listConfigs(tenantId)
    }

    @GetMapping("/{key}")
    fun getConfig(
        @PathVariable key: String,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): String? {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        return configService.getConfig(tenantId, key)
    }

    @PutMapping("/{key}")
    fun upsertConfig(
        @PathVariable key: String,
        @RequestBody request: UpsertConfigRequest,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): TenantConfig {
        val tenantId = user.tenantId ?: throw IllegalStateException("User has no tenant")
        return configService.upsertConfig(tenantId, key, request.configJson)
    }
}
