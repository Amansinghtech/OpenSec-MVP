package com.newklio.opensec.filter

import com.newklio.opensec.context.RequestContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Runs first on every request: assigns (or propagates) a correlation id, exposes it via the
 * response header and MDC (so every log line is traceable), and clears the request context at the
 * end. Every downstream event carries this id per the PRD.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId =
            request
                .getHeader(RequestContext.CORRELATION_ID_HEADER)
                ?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()

        RequestContext.setCorrelationId(correlationId)
        MDC.put(RequestContext.MDC_CORRELATION_ID, correlationId)
        response.setHeader(RequestContext.CORRELATION_ID_HEADER, correlationId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            RequestContext.clear()
            MDC.remove(RequestContext.MDC_CORRELATION_ID)
            MDC.remove(RequestContext.MDC_TENANT_ID)
        }
    }
}
