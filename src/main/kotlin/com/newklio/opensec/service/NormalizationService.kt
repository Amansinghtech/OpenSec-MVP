package com.newklio.opensec.service

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.entity.NormalizedEventRecord
import com.newklio.opensec.repository.NormalizedEventRepository
import com.newklio.opensec.service.parser.LogParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

const val NORMALIZED_EVENT = "normalized_event"

/**
 * Converts raw ingestion envelopes into structured [NormalizedEvent]s (PRD §4): selects a parser by
 * source, persists the result, and publishes it to `normalized_events` for downstream consumers.
 */
@Service
class NormalizationService(
    parsers: List<LogParser>,
    private val normalizedEventRepository: NormalizedEventRepository,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper,
) {
    // Injected in @Order sequence: source-specific parsers first, GenericLogParser last (fallback).
    private val parsers = parsers
    private val log = LoggerFactory.getLogger(NormalizationService::class.java)

    @Transactional
    fun normalize(envelope: EventEnvelope): NormalizedEvent {
        val source = envelope.source ?: "unknown"
        val parser = parsers.first { it.supports(source) }
        val normalized = parser.parse(envelope)

        if (normalizedEventRepository.existsById(normalized.eventId)) {
            log.debug("Normalized event {} already processed; skipping", normalized.eventId)
            return normalized
        }

        normalizedEventRepository.save(toRecord(normalized))
        publish(normalized)
        return normalized
    }

    private fun publish(normalized: NormalizedEvent) {
        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.convertValue(normalized, Map::class.java) as Map<String, Any?>
        val envelope =
            EventEnvelope(
                eventType = NORMALIZED_EVENT,
                tenantId = normalized.tenantId,
                correlationId = normalized.correlationId,
                source = normalized.source,
                occurredAt = normalized.occurredAt,
                payload = payload,
                eventId = normalized.eventId,
            )
        eventPublisher.publish(KafkaTopics.NORMALIZED_EVENTS, envelope)
    }

    private fun toRecord(event: NormalizedEvent): NormalizedEventRecord =
        NormalizedEventRecord(
            id = event.eventId,
            tenantId = event.tenantId,
            correlationId = event.correlationId,
            source = event.source,
            eventType = event.eventType,
            category = event.category,
            severity = event.severity,
            host = event.host,
            entityType = event.entityType,
            entityId = event.entityId,
            occurredAt = event.occurredAt,
            attributes = objectMapper.writeValueAsString(event.attributes),
        )
}
