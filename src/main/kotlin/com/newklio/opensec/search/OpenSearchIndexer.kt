package com.newklio.opensec.search

import com.newklio.opensec.config.OpenSearchProperties
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SessionSummary
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["opensec.opensearch.enabled"], havingValue = "true")
class OpenSearchIndexer(
    properties: OpenSearchProperties,
    private val objectMapper: ObjectMapper,
) : SearchIndexer {
    private val log = LoggerFactory.getLogger(OpenSearchIndexer::class.java)
    private val baseUrl = "${properties.scheme}://${properties.host}:${properties.port}"
    private val http = HttpClient.newHttpClient()

    override fun indexNormalizedEvent(event: NormalizedEvent) {
        index(
            index = INDICES[SearchDocumentType.NORMALIZED_EVENT]!!,
            id = event.eventId.toString(),
            document =
                mapOf(
                    "type" to SearchDocumentType.NORMALIZED_EVENT.name,
                    "tenantId" to event.tenantId.toString(),
                    "summary" to "${event.eventType} on ${event.entityId}",
                    "occurredAt" to event.occurredAt.toString(),
                    "payload" to objectMapper.convertValue(event, Map::class.java),
                ),
        )
    }

    override fun indexSession(session: SessionSummary) {
        index(
            index = INDICES[SearchDocumentType.SESSION]!!,
            id = session.sessionId.toString(),
            document =
                mapOf(
                    "type" to SearchDocumentType.SESSION.name,
                    "tenantId" to session.tenantId.toString(),
                    "summary" to "Session ${session.entityId} (${session.status})",
                    "occurredAt" to session.lastEventAt.toString(),
                    "payload" to objectMapper.convertValue(session, Map::class.java),
                ),
        )
    }

    override fun indexDetectionSignal(signal: DetectionSignal) {
        index(
            index = INDICES[SearchDocumentType.DETECTION_SIGNAL]!!,
            id = signal.signalId.toString(),
            document =
                mapOf(
                    "type" to SearchDocumentType.DETECTION_SIGNAL.name,
                    "tenantId" to signal.tenantId.toString(),
                    "summary" to signal.reason,
                    "occurredAt" to signal.occurredAt.toString(),
                    "payload" to objectMapper.convertValue(signal, Map::class.java),
                ),
        )
    }

    override fun search(
        tenantId: UUID,
        query: String,
        type: SearchDocumentType?,
        from: Int,
        size: Int,
    ): SearchResult {
        val indices =
            if (type != null) {
                INDICES[type]!!
            } else {
                INDICES.values.joinToString(",")
            }

        val body =
            mapOf(
                "from" to from,
                "size" to size,
                "query" to
                    mapOf(
                        "bool" to
                            mapOf(
                                "must" to
                                    listOf(
                                        mapOf("term" to mapOf("tenantId" to tenantId.toString())),
                                        mapOf(
                                            "multi_match" to
                                                mapOf(
                                                    "query" to query,
                                                    "fields" to listOf("summary", "payload"),
                                                ),
                                        ),
                                    ),
                            ),
                    ),
            )

        val response =
            post("$baseUrl/$indices/_search", objectMapper.writeValueAsString(body))
                ?: return SearchResult(0, emptyList())

        @Suppress("UNCHECKED_CAST")
        val root = objectMapper.readValue(response, Map::class.java) as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val hitsWrapper = root["hits"] as? Map<String, Any?> ?: return SearchResult(0, emptyList())

        @Suppress("UNCHECKED_CAST")
        val hits = hitsWrapper["hits"] as? List<Map<String, Any?>> ?: emptyList()
        val total =
            (hitsWrapper["total"] as? Map<String, Any?>)
                ?.get("value")
                ?.toString()
                ?.toLongOrNull() ?: hits.size.toLong()

        val mapped =
            hits.mapNotNull { hit ->
                @Suppress("UNCHECKED_CAST")
                val source = hit["_source"] as? Map<String, Any?> ?: return@mapNotNull null
                SearchHit(
                    id = hit["_id"] as? String ?: "",
                    type = SearchDocumentType.valueOf(source["type"] as String),
                    tenantId = UUID.fromString(source["tenantId"] as String),
                    score = (hit["_score"] as? Number)?.toFloat() ?: 0f,
                    summary = source["summary"] as? String ?: "",
                    occurredAt = source["occurredAt"] as? String,
                    payload = source["payload"] as? Map<String, Any?> ?: emptyMap(),
                )
            }
        return SearchResult(total = total, hits = mapped)
    }

    private fun index(
        index: String,
        id: String,
        document: Map<String, Any?>,
    ) {
        runCatching {
            ensureIndex(index)
            put("$baseUrl/$index/_doc/$id", objectMapper.writeValueAsString(document))
        }.onFailure { ex ->
            log.warn("OpenSearch index failed for {}:{} — {}", index, id, ex.message)
        }
    }

    private fun ensureIndex(index: String) {
        val head =
            http.send(
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("$baseUrl/$index"))
                    .HEAD()
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        if (head.statusCode() == 404) {
            put("$baseUrl/$index", "{}")
        }
    }

    private fun put(
        url: String,
        body: String,
    ) {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build()
        http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun post(
        url: String,
        body: String,
    ): String? {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return if (response.statusCode() in 200..299) response.body() else null
    }

    companion object {
        private val INDICES =
            mapOf(
                SearchDocumentType.NORMALIZED_EVENT to "opensec-normalized-events",
                SearchDocumentType.SESSION to "opensec-sessions",
                SearchDocumentType.DETECTION_SIGNAL to "opensec-detection-signals",
            )
    }
}
