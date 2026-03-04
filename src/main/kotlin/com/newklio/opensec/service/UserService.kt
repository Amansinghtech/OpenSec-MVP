package com.newklio.opensec.service

import com.newklio.opensec.dto.UserRequest
import com.newklio.opensec.entity.User
import com.newklio.opensec.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {


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