package com.newklio.opensec.service

import com.newklio.opensec.dto.UserRequest
import com.newklio.opensec.entity.User
import com.newklio.opensec.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
): UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {

        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found")

        return org.springframework.security.core.userdetails.User(
            user.username,
            user.password,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    fun getUsers(): List<User> {
        return userRepository.findAll()
    }

    fun getUser(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun deleteUser(id: Long) {
        userRepository.deleteById(id)
    }
}