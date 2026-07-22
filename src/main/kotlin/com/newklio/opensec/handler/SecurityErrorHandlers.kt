package com.newklio.opensec.handler

import com.newklio.opensec.dto.ApiError
import tools.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        writeError(response, request, HttpStatus.UNAUTHORIZED, "Authentication required", objectMapper)
    }
}

@Component
class RestAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        writeError(response, request, HttpStatus.FORBIDDEN, "Access denied", objectMapper)
    }
}

private fun writeError(
    response: HttpServletResponse,
    request: HttpServletRequest,
    status: HttpStatus,
    message: String,
    objectMapper: ObjectMapper
) {
    response.status = status.value()
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    val body = ApiError(
        status = status.value(),
        error = status.reasonPhrase,
        message = message,
        path = request.requestURI
    )
    response.writer.write(objectMapper.writeValueAsString(body))
}
