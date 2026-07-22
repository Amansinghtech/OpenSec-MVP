package com.newklio.opensec.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "roles")
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false, unique = true)
    val name: String,
    @Column
    val description: String? = null,
) {
    // Declared outside the primary constructor so the collection is excluded from
    // data-class equals/hashCode/toString (avoids Hibernate lazy/PersistentSet pitfalls).
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")],
    )
    var permissions: MutableSet<Permission> = mutableSetOf()
}
