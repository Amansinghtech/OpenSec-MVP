package com.newklio.opensec.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
        Index(name = "idx_user_phone", columnList = "phone"),
    ],
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
    @field:JsonIgnore
    @Column(nullable = false)
    var password: String,
    @Column(nullable = false)
    var enabled: Boolean = true,
    @CreationTimestamp
    val createdAt: Instant? = null,
    @UpdateTimestamp
    val updatedAt: Instant? = null,
) {
    // Relations declared outside the primary constructor so they are excluded from the
    // data-class equals/hashCode/toString/copy (avoids Hibernate lazy-collection pitfalls).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    var tenant: Tenant? = null

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = mutableSetOf()
}

class AuthenticatedUser(
    val details: User,
) : UserDetails {
    override fun getUsername() = details.username

    override fun getPassword() = details.password

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()
        details.roles.forEach { role ->
            authorities.add(SimpleGrantedAuthority("ROLE_${role.name}"))
            role.permissions.forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission.name))
            }
        }
        return authorities
    }

    override fun isEnabled() = details.enabled

    val tenantId: UUID? get() = details.tenant?.id
}
