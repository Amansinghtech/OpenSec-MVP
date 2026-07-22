package com.newklio.opensec.controller

import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.entity.User
import com.newklio.opensec.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal authenticatedUser: AuthenticatedUser): User {
        return authenticatedUser.details
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listUsers(): List<User> {
        return userService.getUsers()
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUser(@PathVariable id: UUID): ResponseEntity<User> {
        val user = userService.getUser(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        userService.deleteUser(id)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
