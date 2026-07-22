package com.newklio.opensec.search

import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SessionSummary
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["opensec.opensearch.enabled"], havingValue = "false", matchIfMissing = true)
class NoopSearchIndexer : SearchIndexer {
    override fun indexNormalizedEvent(event: NormalizedEvent) = Unit

    override fun indexSession(session: SessionSummary) = Unit

    override fun indexDetectionSignal(signal: DetectionSignal) = Unit

    override fun search(
        tenantId: UUID,
        query: String,
        type: SearchDocumentType?,
        from: Int,
        size: Int,
    ): SearchResult = SearchResult(total = 0, hits = emptyList())
}
