package com.newklio.opensec

import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SignalSeverity
import com.newklio.opensec.rag.InMemoryVectorStore
import com.newklio.opensec.service.RagEnrichmentService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["opensec.rag.enabled=true"])
class RagEnrichmentServiceTest
    @Autowired
    constructor(
        private val ragEnrichmentService: RagEnrichmentService,
        private val vectorStore: InMemoryVectorStore,
    ) {
        @Test
        fun `rag enriches detection with cve context`() {
            val signal =
                DetectionSignal(
                    signalId = UUID.randomUUID(),
                    tenantId = UUID.randomUUID(),
                    detector = "signature",
                    signalType = "log4j_probe",
                    entityType = EntityType.HOST,
                    entityId = "web-1",
                    riskScore = 85,
                    severity = SignalSeverity.HIGH,
                    reason = "Log4Shell JNDI lookup attempt detected",
                    occurredAt = Instant.now(),
                )

            val result = ragEnrichmentService.enrich(signal)
            assertNotNull(result)
            assertTrue(result.cveIds.isNotEmpty())
        }

        @Test
        fun `vector store returns log4shell match`() {
            val hits = vectorStore.search("Log4j remote code", 2)
            assertTrue(hits.any { it.document.cveId == "CVE-2021-44228" })
        }
    }
