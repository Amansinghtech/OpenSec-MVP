package com.newklio.opensec.filter

import com.newklio.opensec.config.RateLimitProperties
import com.newklio.opensec.dto.ApiError
import com.newklio.opensec.service.RateLimiter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

/**
 * Applies Redis-backed rate limiting to configured path prefixes (auth endpoints by default).
 * Only registered when `opensec.rate-limit.enabled=true`.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = ["opensec.rate-limit.enabled"], havingValue = "true")
class RateLimitingFilter(
    private val rateLimiter: RateLimiter,
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI
        if (properties.pathPrefixes.none { path.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val key = "ratelimit:$path:${clientIp(request)}"
        if (rateLimiter.allow(key)) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val body =
            ApiError(
                status = HttpStatus.TOO_MANY_REQUESTS.value(),
                error = HttpStatus.TOO_MANY_REQUESTS.reasonPhrase,
                message = "Rate limit exceeded. Try again later.",
                path = path,
            )
        response.writer.write(objectMapper.writeValueAsString(body))
    }

    private fun clientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) forwarded.split(",").first().trim() else request.remoteAddr
    }
}
