package com.newklio.opensec.filter

import com.newklio.opensec.entity.AgentPrincipal
import com.newklio.opensec.service.AgentService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates enrolled OPENSEC agents via API key. Accepts:
 * - `Authorization: ApiKey <key>`
 * - `X-Api-Key: <key>`
 *
 * Runs before JWT auth so agents can ingest telemetry without a user login.
 */
@Component
class AgentApiKeyAuthFilter(
    private val agentService: AgentService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val apiKey = extractApiKey(request)
        if (apiKey != null) {
            val agent = agentService.authenticateByApiKey(apiKey)
            if (agent != null) {
                val principal = AgentPrincipal(agent)
                val authToken =
                    UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.authorities,
                    )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractApiKey(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("ApiKey ", ignoreCase = true)) {
            return header.substring(7).trim().takeIf { it.isNotEmpty() }
        }
        return request.getHeader("X-Api-Key")?.trim()?.takeIf { it.isNotEmpty() }
    }
}
