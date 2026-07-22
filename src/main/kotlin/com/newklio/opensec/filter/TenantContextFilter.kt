package com.newklio.opensec.filter

import com.newklio.opensec.context.RequestContext
import com.newklio.opensec.entity.AuthenticatedUser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs after authentication (lowest precedence) and binds the authenticated user's tenant to the
 * request context + MDC, so downstream services can scope work by tenant transparently.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class TenantContextFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        if (principal is AuthenticatedUser) {
            val tenantId = principal.tenantId
            RequestContext.setTenantId(tenantId)
            if (tenantId != null) {
                MDC.put(RequestContext.MDC_TENANT_ID, tenantId.toString())
            }
        }
        filterChain.doFilter(request, response)
    }
}
