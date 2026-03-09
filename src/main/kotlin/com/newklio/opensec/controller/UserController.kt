package com.newklio.opensec.controller

import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.entity.User
import com.newklio.opensec.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {


    @GetMapping("/me")
    fun getUser(@AuthenticationPrincipal authenticatedUser: AuthenticatedUser): User? {
        return authenticatedUser.details
    }

}