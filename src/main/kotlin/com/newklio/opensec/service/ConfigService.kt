package com.newklio.opensec.service

import com.newklio.opensec.entity.TenantConfig
import com.newklio.opensec.repository.TenantConfigRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.TimeUnit

const val CONFIG_CACHE_PREFIX = "config:tenant:"

@Service
class ConfigService(
    private val tenantConfigRepository: TenantConfigRepository,
    @Autowired(required = false) private val redisTemplate: StringRedisTemplate?,
) {
    @Transactional(readOnly = true)
    fun getConfig(
        tenantId: UUID,
        configKey: String,
    ): String? {
        cacheKey(tenantId, configKey)?.let { key ->
            runCatching { redisTemplate?.opsForValue()?.get(key) }.getOrNull()?.let { return it }
        }
        return tenantConfigRepository.findByTenantIdAndConfigKey(tenantId, configKey)?.configJson
    }

    @Transactional(readOnly = true)
    fun listConfigs(tenantId: UUID): Map<String, String> =
        tenantConfigRepository.findByTenantId(tenantId).associate { it.configKey to it.configJson }

    @Transactional
    fun upsertConfig(
        tenantId: UUID,
        configKey: String,
        configJson: String,
    ): TenantConfig {
        val existing = tenantConfigRepository.findByTenantIdAndConfigKey(tenantId, configKey)
        val saved =
            if (existing != null) {
                existing.configJson = configJson
                tenantConfigRepository.save(existing)
            } else {
                tenantConfigRepository.save(TenantConfig(tenantId = tenantId, configKey = configKey, configJson = configJson))
            }
        pushToCache(tenantId, configKey, configJson)
        return saved
    }

    private fun pushToCache(
        tenantId: UUID,
        configKey: String,
        configJson: String,
    ) {
        val key = cacheKey(tenantId, configKey) ?: return
        runCatching {
            redisTemplate?.opsForValue()?.set(key, configJson, 1, TimeUnit.HOURS)
        }
    }

    private fun cacheKey(
        tenantId: UUID,
        configKey: String,
    ): String? = if (redisTemplate != null) "$CONFIG_CACHE_PREFIX$tenantId:$configKey" else null
}
