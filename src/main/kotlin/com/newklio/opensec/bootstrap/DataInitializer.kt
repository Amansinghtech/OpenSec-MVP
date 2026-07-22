package com.newklio.opensec.bootstrap

import com.newklio.opensec.config.BootstrapConfig
import com.newklio.opensec.entity.Permission
import com.newklio.opensec.entity.Role
import com.newklio.opensec.entity.Tenant
import com.newklio.opensec.entity.User
import com.newklio.opensec.repository.PermissionRepository
import com.newklio.opensec.repository.RoleRepository
import com.newklio.opensec.repository.TenantRepository
import com.newklio.opensec.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Seeds baseline RBAC reference data (permissions, roles, default tenant) and a bootstrap
 * admin user. Idempotent: safe to run on every startup.
 */
@Component
class DataInitializer(
    private val permissionRepository: PermissionRepository,
    private val roleRepository: RoleRepository,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val bootstrapConfig: BootstrapConfig
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    private val permissions = mapOf(
        "USER_READ" to "Read users",
        "USER_WRITE" to "Create or update users",
        "USER_DELETE" to "Delete users",
        "TENANT_READ" to "Read tenants",
        "TENANT_WRITE" to "Create or update tenants",
        "ROLE_READ" to "Read roles",
        "ROLE_WRITE" to "Create or update roles",
        "AUDIT_READ" to "Read audit logs"
    )

    private val roleDefinitions = mapOf(
        "ADMIN" to permissions.keys,
        "ANALYST" to setOf("USER_READ", "TENANT_READ", "ROLE_READ", "AUDIT_READ"),
        "AGENT" to setOf("USER_READ"),
        "VIEWER" to setOf("USER_READ")
    )

    @Transactional
    override fun run(vararg args: String) {
        val permissionsByName = seedPermissions()
        seedRoles(permissionsByName)
        val tenant = seedDefaultTenant()
        seedAdminUser(tenant)
    }

    private fun seedPermissions(): Map<String, Permission> {
        return permissions.entries.associate { (name, description) ->
            val permission = permissionRepository.findByName(name)
                ?: permissionRepository.save(Permission(name = name, description = description))
            name to permission
        }
    }

    private fun seedRoles(permissionsByName: Map<String, Permission>) {
        roleDefinitions.forEach { (roleName, permissionNames) ->
            val role = roleRepository.findByName(roleName) ?: Role(name = roleName, description = "$roleName role")
            val desired = permissionNames.mapNotNull { permissionsByName[it] }.toMutableSet()
            if (role.id == null || role.permissions != desired) {
                role.permissions = desired
                roleRepository.save(role)
            }
        }
    }

    private fun seedDefaultTenant(): Tenant {
        return tenantRepository.findBySlug(bootstrapConfig.defaultTenantSlug)
            ?: tenantRepository.save(
                Tenant(
                    name = bootstrapConfig.defaultTenantName,
                    slug = bootstrapConfig.defaultTenantSlug
                )
            )
    }

    private fun seedAdminUser(tenant: Tenant) {
        if (userRepository.existsByUsername(bootstrapConfig.adminUsername)) {
            return
        }
        val adminRole = roleRepository.findByName("ADMIN")
            ?: error("ADMIN role must be seeded before the admin user")

        val admin = User(
            username = bootstrapConfig.adminUsername,
            password = passwordEncoder.encode(bootstrapConfig.adminPassword)!!,
            email = bootstrapConfig.adminEmail,
            phone = bootstrapConfig.adminPhone
        )
        admin.tenant = tenant
        admin.roles.add(adminRole)
        userRepository.save(admin)
        log.warn(
            "Bootstrap admin user '{}' created. Change its password immediately via BOOTSTRAP_ADMIN_PASSWORD.",
            bootstrapConfig.adminUsername
        )
    }
}
