package com.newklio.opensec.entity


import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserEntityListener(
    private val passwordEncoder: PasswordEncoder
) {

    @PrePersist
    fun beforeInsert(user: User) {
        encodePassword(user)
    }

    @PreUpdate
    fun beforeUpdate(user: User) {
        encodePassword(user)
    }

    private fun encodePassword(user: User) {

        if (!user.password.startsWith("\$2a\$") &&
            !user.password.startsWith("\$2b\$")
        ) {
            user.password = passwordEncoder.encode(user.password)!!
        }
    }
}