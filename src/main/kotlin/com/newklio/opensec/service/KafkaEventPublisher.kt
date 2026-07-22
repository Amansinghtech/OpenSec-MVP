package com.newklio.opensec.service

import com.newklio.opensec.dto.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.ObjectMapper

/**
 * Kafka-backed event publisher. Serializes the envelope to JSON and keys records by `tenant_id`
 * (so a tenant's events land on the same partition and stay ordered). Sends asynchronously and
 * logs — never throws — on failure, so the ingestion/request path is never blocked by the bus.
 */
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : EventPublisher {
    private val log = LoggerFactory.getLogger(KafkaEventPublisher::class.java)

    override fun publish(
        topic: String,
        envelope: EventEnvelope,
    ) {
        try {
            val json = objectMapper.writeValueAsString(envelope)
            kafkaTemplate
                .send(topic, envelope.tenantId.toString(), json)
                .whenComplete { _, ex ->
                    if (ex != null) {
                        log.warn("Failed to publish {} to topic {}: {}", envelope.eventType, topic, ex.message)
                    }
                }
        } catch (ex: Exception) {
            log.warn("Error publishing {} to topic {}: {}", envelope.eventType, topic, ex.message)
        }
    }
}
