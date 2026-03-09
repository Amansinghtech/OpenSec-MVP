package com.newklio.opensec.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant
import java.util.*

@Entity
@EntityListeners(UserEntityListener::class)
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_username", columnList = "username"),
        Index(name = "idx_user_email", columnList = "email"),
        Index(name = "idx_user_phone", columnList = "phone")
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val username: String,

    @Column(unique = false, nullable = true)
    val email: String,

    @Column(nullable = false, length = 15)
    val phone: String,

    @Column(nullable = true)
    val fullName: String? = null,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @CreationTimestamp
    val createdAt: Instant? = null,

    @UpdateTimestamp
    val updatedAt: Instant? = null
)

class AuthenticatedUser(val details: User) : UserDetails {
    override fun getUsername() = details.username
    override fun getPassword() = details.password
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
}