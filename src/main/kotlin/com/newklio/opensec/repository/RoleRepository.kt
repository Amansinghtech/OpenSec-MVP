package com.newklio.opensec.repository

import com.newklio.opensec.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RoleRepository : JpaRepository<Role, UUID> {
    fun findByName(name: String): Role?
}
