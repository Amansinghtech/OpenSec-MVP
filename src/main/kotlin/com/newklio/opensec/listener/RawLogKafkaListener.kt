package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.service.NormalizationService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Consumes raw ingestion events from Kafka and hands them to normalization. Only active when the
 * event bus is enabled; the core logic lives in [NormalizationService] and is unit/integration
 * tested independently of Kafka.
 */
@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class RawLogKafkaListener(
    private val normalizationService: NormalizationService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(RawLogKafkaListener::class.java)

    @KafkaListener(topics = [KafkaTopics.RAW_LOGS], groupId = "\${opensec.kafka.consumer-group:opensec}-normalization")
    fun onMessage(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            normalizationService.normalize(envelope)
        } catch (ex: Exception) {
            log.error("Failed to normalize raw log event: {}", ex.message, ex)
        }
    }
}
