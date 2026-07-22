package com.newklio.opensec

import com.newklio.opensec.repository.TenantRepository
import com.newklio.opensec.service.ConfigService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class ConfigServiceTest
    @Autowired
    constructor(
        private val configService: ConfigService,
        private val tenantRepository: TenantRepository,
    ) {
        @Test
        fun `upsert and read tenant config`() {
            val tenantId = tenantRepository.findAll().first().id!!
            val json = """{"emitThreshold":75}"""
            configService.upsertConfig(tenantId, "detection", json)
            assertEquals(json, configService.getConfig(tenantId, "detection"))
            assertNotNull(configService.listConfigs(tenantId)["detection"])
        }
    }
