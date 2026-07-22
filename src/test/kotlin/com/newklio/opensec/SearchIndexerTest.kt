package com.newklio.opensec

import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.model.SignalSeverity
import com.newklio.opensec.search.SearchIndexer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
class SearchIndexerTest
    @Autowired
    constructor(
        private val searchIndexer: SearchIndexer,
    ) {
        @Test
        fun `noop indexer accepts documents and returns empty search`() {
            val tenantId = UUID.randomUUID()
            searchIndexer.indexNormalizedEvent(
                NormalizedEvent(
                    eventId = UUID.randomUUID(),
                    tenantId = tenantId,
                    source = "wazuh",
                    eventType = NormalizedEventType.AUTH_FAILURE,
                    entityType = EntityType.USER,
                    entityId = "admin",
                    occurredAt = Instant.now(),
                ),
            )
            val result = searchIndexer.search(tenantId, "admin")
            assertEquals(0, result.total)
        }

        @Test
        fun `noop indexer indexes detection signal without error`() {
            searchIndexer.indexDetectionSignal(
                DetectionSignal(
                    tenantId = UUID.randomUUID(),
                    detector = "signature",
                    signalType = "test",
                    entityType = EntityType.HOST,
                    entityId = "host-1",
                    riskScore = 80,
                    severity = SignalSeverity.HIGH,
                    reason = "test signal",
                    occurredAt = Instant.now(),
                ),
            )
        }
    }
