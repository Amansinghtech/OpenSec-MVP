package com.newklio.opensec

import com.newklio.opensec.dto.CreateAgentRequest
import com.newklio.opensec.dto.HeartbeatRequest
import com.newklio.opensec.model.AgentPlatform
import com.newklio.opensec.model.AgentStatus
import com.newklio.opensec.repository.AgentRepository
import com.newklio.opensec.repository.TenantRepository
import com.newklio.opensec.service.AgentService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
class AgentServiceTest
    @Autowired
    constructor(
        private val agentService: AgentService,
        private val agentRepository: AgentRepository,
        private val tenantRepository: TenantRepository,
    ) {
        @Test
        fun `heartbeat refuses to reactivate a revoked agent`() {
            val tenantId = tenantRepository.findAll().first().id!!
            val enrolled =
                agentService.enroll(
                    CreateAgentRequest(
                        name = "race-${UUID.randomUUID().toString().take(8)}",
                        platform = AgentPlatform.LINUX,
                    ),
                    tenantId,
                )

            assertEquals(true, agentService.revoke(enrolled.agent.id, tenantId))

            assertThrows<AccessDeniedException> {
                agentService.heartbeat(enrolled.agent.id, HeartbeatRequest())
            }

            val stored = agentRepository.findById(enrolled.agent.id).get()
            assertEquals(AgentStatus.REVOKED, stored.status)
        }
    }
