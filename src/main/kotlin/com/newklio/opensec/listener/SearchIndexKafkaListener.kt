package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.DETECTION_SIGNAL_EVENT
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SESSION_UPDATED_EVENT
import com.newklio.opensec.dto.SessionSummary
import com.newklio.opensec.search.SearchIndexer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Indexes normalized events, sessions, and detection signals into OpenSearch for timeline/search
 * queries. Failures are logged and do not block the pipeline.
 */
@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class SearchIndexKafkaListener(
    private val searchIndexer: SearchIndexer,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(SearchIndexKafkaListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.NORMALIZED_EVENTS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-search-index",
    )
    fun onNormalizedEvent(message: String) {
        runCatching {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            val event = objectMapper.convertValue(envelope.payload, NormalizedEvent::class.java)
            searchIndexer.indexNormalizedEvent(event)
        }.onFailure { ex ->
            log.warn("Search index failed for normalized event: {}", ex.message)
        }
    }

    @KafkaListener(
        topics = [KafkaTopics.SESSION_EVENTS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-search-index",
    )
    fun onSessionEvent(message: String) {
        runCatching {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != SESSION_UPDATED_EVENT) return
            val session = objectMapper.convertValue(envelope.payload, SessionSummary::class.java)
            searchIndexer.indexSession(session)
        }.onFailure { ex ->
            log.warn("Search index failed for session event: {}", ex.message)
        }
    }

    @KafkaListener(
        topics = [KafkaTopics.DETECTION_SIGNALS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-search-index",
    )
    fun onDetectionSignal(message: String) {
        runCatching {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != DETECTION_SIGNAL_EVENT) return
            val signal = objectMapper.convertValue(envelope.payload, DetectionSignal::class.java)
            searchIndexer.indexDetectionSignal(signal)
        }.onFailure { ex ->
            log.warn("Search index failed for detection signal: {}", ex.message)
        }
    }
}
