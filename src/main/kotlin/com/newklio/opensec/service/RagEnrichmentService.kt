package com.newklio.opensec.service

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.CONTEXTUALIZED_DETECTION_EVENT
import com.newklio.opensec.dto.ContextualizedDetection
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.rag.LlmProvider
import com.newklio.opensec.rag.RagProperties
import com.newklio.opensec.rag.VectorStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class RagEnrichmentService(
    private val vectorStore: VectorStore,
    private val llmProvider: LlmProvider,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper,
    private val properties: RagProperties,
) {
    private val log = LoggerFactory.getLogger(RagEnrichmentService::class.java)

    fun enrich(signal: DetectionSignal): ContextualizedDetection? {
        if (!properties.enabled) return null
        return runCatching {
            val hits = vectorStore.search(signal.reason, properties.topK)
            val exploits = hits.map { it.document }
            val contextualized =
                ContextualizedDetection(
                    signalId = signal.signalId,
                    tenantId = signal.tenantId,
                    correlationId = signal.correlationId,
                    detector = signal.detector,
                    riskScore = signal.riskScore,
                    severity = signal.severity,
                    reason = signal.reason,
                    cveIds = exploits.map { it.cveId },
                    contextSummary = llmProvider.enrich(signal, exploits),
                    occurredAt = signal.occurredAt,
                )
            publish(contextualized)
            contextualized
        }.onFailure { ex ->
            log.warn("RAG enrichment failed for signal {}: {}", signal.signalId, ex.message)
        }.getOrNull()
    }

    private fun publish(detection: ContextualizedDetection) {
        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.convertValue(detection, Map::class.java) as Map<String, Any?>
        eventPublisher.publish(
            KafkaTopics.CONTEXTUALIZED_DETECTIONS,
            EventEnvelope(
                eventType = CONTEXTUALIZED_DETECTION_EVENT,
                tenantId = detection.tenantId,
                correlationId = detection.correlationId,
                source = "rag",
                occurredAt = detection.occurredAt,
                payload = payload,
                eventId = detection.id,
            ),
        )
    }
}
