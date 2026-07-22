package com.newklio.opensec.handler

import com.newklio.opensec.dto.ApiError
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "invalid value")
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors)
    }

    @ExceptionHandler(BadCredentialsException::class, AuthenticationException::class)
    fun handleAuthentication(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request)
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUserNotFound(
        ex: UsernameNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return build(HttpStatus.NOT_FOUND, ex.message ?: "User not found", request)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return build(HttpStatus.CONFLICT, ex.message ?: "Conflict", request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return build(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request)
    }

    private fun build(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        fieldErrors: Map<String, String>? = null
    ): ResponseEntity<ApiError> {
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI,
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(status).body(body)
    }
}
