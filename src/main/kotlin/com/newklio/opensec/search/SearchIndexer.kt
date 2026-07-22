package com.newklio.opensec.search

import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SessionSummary
import java.util.UUID

enum class SearchDocumentType {
    NORMALIZED_EVENT,
    SESSION,
    DETECTION_SIGNAL,
}

data class SearchHit(
    val id: String,
    val type: SearchDocumentType,
    val tenantId: UUID,
    val score: Float,
    val summary: String,
    val occurredAt: String?,
    val payload: Map<String, Any?>,
)

data class SearchResult(
    val total: Long,
    val hits: List<SearchHit>,
)

interface SearchIndexer {
    fun indexNormalizedEvent(event: NormalizedEvent)

    fun indexSession(session: SessionSummary)

    fun indexDetectionSignal(signal: DetectionSignal)

    fun search(
        tenantId: UUID,
        query: String,
        type: SearchDocumentType? = null,
        from: Int = 0,
        size: Int = 20,
    ): SearchResult
}
